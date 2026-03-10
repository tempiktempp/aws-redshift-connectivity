package com.aws.utils.redshift.connection;

import com.aws.utils.redshift.config.RedshiftProperties;
import com.aws.utils.redshift.exception.RedshiftQueryException;
import com.aws.utils.redshift.exception.RedshiftTimeoutException;
import com.aws.utils.redshift.model.QueryRequest;
import com.aws.utils.redshift.model.QueryResult;
import com.aws.utils.redshift.model.QueryResult.ColumnMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient;
import software.amazon.awssdk.services.redshiftdata.model.CancelStatementRequest;
import software.amazon.awssdk.services.redshiftdata.model.CancelStatementResponse;
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest;
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementResponse;
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse;
import software.amazon.awssdk.services.redshiftdata.model.Field;
import software.amazon.awssdk.services.redshiftdata.model.GetStatementResultRequest;
import software.amazon.awssdk.services.redshiftdata.model.GetStatementResultResponse;
import software.amazon.awssdk.services.redshiftdata.model.RedshiftDataException;
import software.amazon.awssdk.services.redshiftdata.model.SqlParameter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redshift connector backed by the AWS Redshift Data API.
 *
 * How it works:
 *   1. Submits query via ExecuteStatement — returns a query ID immediately
 *   2. Polls DescribeStatement until status = FINISHED, FAILED or ABORTED
 *   3. Fetches results via GetStatementResult with automatic pagination
 *
 * Security properties:
 *   - Cluster can be in a fully private subnet — no inbound rules needed
 *   - IAM-native — no passwords stored or transmitted
 *   - Parameters are bound server-side — no SQL injection risk from values
 *   - All API calls are automatically logged in AWS CloudTrail
 *
 * Trade-off:
 *   Asynchronous polling adds ~200-500ms overhead vs direct JDBC.
 *   Use JdbcRedshiftConnector if sub-100ms latency is required
 *   and direct cluster access is acceptable in your network setup.
 */
@Slf4j
@RequiredArgsConstructor
public class DataApiRedshiftConnector implements RedshiftConnector {

    private final RedshiftDataClient redshiftDataClient;
    private final RedshiftProperties props;

    private static final String STRATEGY_NAME = "DATA_API";

    // ── RedshiftConnector implementation ──────────────────────────

    @Override
    public QueryResult executeQuery(QueryRequest request) {
        log.debug("[Redshift-DataAPI] Executing query, " +
                "database: {}", props.getDatabase());

        String queryId = submitQuery(request);
        log.debug("[Redshift-DataAPI] Query submitted, id: {}",
                queryId);

        waitForCompletion(queryId, request);

        QueryResult result = fetchResults(queryId, request);
        log.debug("[Redshift-DataAPI] Query complete, " +
                "rows: {}", result.getTotalRows());

        return result;
    }

    @Override
    public boolean isHealthy() {
        try {
            QueryRequest request = QueryRequest.builder()
                    .sql("SELECT 1")
                    .queryLabel("health-check")
                    .build();
            QueryResult result = executeQuery(request);
            return !result.isEmpty();
        } catch (Exception e) {
            log.warn("[Redshift-DataAPI] Health check failed: {}",
                    e.getMessage());
            return false;
        }
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    // ── Step 1: Submit query ───────────────────────────────────────

    private String submitQuery(QueryRequest request) {
        ExecuteStatementRequest.Builder builder =
                ExecuteStatementRequest.builder()
                        .database(props.getDatabase())
                        .sql(request.getSql());

        // Target: provisioned cluster OR serverless workgroup
        // These are mutually exclusive — one must be set
        if (hasValue(props.getClusterIdentifier())) {
            builder.clusterIdentifier(props.getClusterIdentifier());
        } else if (hasValue(props.getWorkgroupName())) {
            builder.workgroupName(props.getWorkgroupName());
        } else {
            throw new RedshiftQueryException(
                    "Either clusterIdentifier or workgroupName " +
                    "must be configured in redshift properties.");
        }

        // Auth: Secrets Manager ARN takes priority over dbUser
        if (hasValue(props.getSecretArn())) {
            builder.secretArn(props.getSecretArn());
        } else if (hasValue(props.getDbUser())) {
            builder.dbUser(props.getDbUser());
        }

        // Bind named parameters server-side
        // Values never touch the SQL string — eliminates injection risk
        if (request.getParameters() != null
                && !request.getParameters().isEmpty()) {
            List<SqlParameter> sqlParams = request.getParameters()
                    .entrySet().stream()
                    .map(e -> SqlParameter.builder()
                            .name(e.getKey())
                            .value(e.getValue() != null
                                    ? e.getValue().toString()
                                    : null)
                            .build())
                    .toList();
            builder.parameters(sqlParams);
        }

        try {
            ExecuteStatementResponse response =
                    redshiftDataClient.executeStatement(builder.build());
            return response.id();
        } catch (RedshiftDataException e) {
            throw new RedshiftQueryException(
                    "Failed to submit query to Redshift Data API: "
                    + e.getMessage(), e);
        }
    }

    // ── Step 2: Poll until complete ────────────────────────────────

    private void waitForCompletion(String queryId,
                                    QueryRequest request) {
        int timeoutSeconds = request.getTimeoutSeconds() > 0
                ? request.getTimeoutSeconds()
                : props.getQuery().getTimeoutSeconds();

        Instant deadline = Instant.now()
                .plusSeconds(timeoutSeconds);

        DescribeStatementRequest describeRequest =
                DescribeStatementRequest.builder()
                        .id(queryId)
                        .build();

        while (true) {

            if (Instant.now().isAfter(deadline)) {
                cancelQuery(queryId);
                throw new RedshiftTimeoutException(
                        String.format(
                                "Query [%s] timed out after %d seconds.",
                                queryId, timeoutSeconds));
            }

            DescribeStatementResponse status =
                    redshiftDataClient.describeStatement(describeRequest);
            String queryStatus = status.statusAsString();

            log.trace("[Redshift-DataAPI] Query {} status: {}",
                    queryId, queryStatus);

            switch (queryStatus) {
                case "FINISHED" -> {
                    return; // success — proceed to fetch
                }
                case "FAILED", "ABORTED" -> throw new RedshiftQueryException(
                        String.format(
                                "Query [%s] %s. Error: %s",
                                queryId,
                                queryStatus,
                                status.error()));
                default -> sleep(props.getQuery().getPollIntervalMs());
                // SUBMITTED / PICKED / STARTED — keep polling
            }
        }
    }

    // ── Step 3: Fetch results ──────────────────────────────────────

    private QueryResult fetchResults(String queryId,
                                      QueryRequest request) {
        List<Map<String, Object>> allRows = new ArrayList<>();
        List<ColumnMetadata> columnMetadata = null;
        String nextToken = null;

        int maxResults = request.getMaxResults() > 0
                ? Math.min(request.getMaxResults(),
                           props.getQuery().getMaxResultsPerPage())
                : props.getQuery().getMaxResultsPerPage();

        do {
            GetStatementResultRequest.Builder resultBuilder =
                    GetStatementResultRequest.builder()
                            .id(queryId);

            if (nextToken != null) {
                resultBuilder.nextToken(nextToken);
            }

            GetStatementResultResponse response =
                    redshiftDataClient.getStatementResult(
                            resultBuilder.build());

            // Extract column metadata on first page only
            if (columnMetadata == null) {
                columnMetadata = extractColumnMetadata(
                        response.columnMetadata());
            }

            // Map each row to columnName -> value
            List<ColumnMetadata> finalMeta = columnMetadata;
            response.records().stream()
                    .map(row -> mapRow(row, finalMeta))
                    .forEach(allRows::add);

            nextToken = response.nextToken();

        } while (nextToken != null
                && !nextToken.isBlank()
                && allRows.size() < maxResults);

        return QueryResult.builder()
                .rows(Collections.unmodifiableList(allRows))
                .columnMetadata(columnMetadata != null
                        ? columnMetadata
                        : Collections.emptyList())
                .totalRows(allRows.size())
                .queryId(queryId)
                .strategy(STRATEGY_NAME)
                .build();
    }

    // ── Mapping helpers ────────────────────────────────────────────

    private List<ColumnMetadata> extractColumnMetadata(
            List<software.amazon.awssdk.services.redshiftdata
                    .model.ColumnMetadata> sdkMeta) {
        return sdkMeta.stream()
                .map(col -> new ColumnMetadata(
                        col.name(), col.typeName()))
                .toList();
    }

    private Map<String, Object> mapRow(
            List<Field> fields,
            List<ColumnMetadata> columns) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            row.put(columns.get(i).name(),
                    extractFieldValue(fields.get(i)));
        }
        return row;
    }

    private Object extractFieldValue(Field field) {
        if (Boolean.TRUE.equals(field.isNull())) return null;
        if (field.stringValue() != null)  return field.stringValue();
        if (field.longValue() != null)    return field.longValue();
        if (field.doubleValue() != null)  return field.doubleValue();
        if (field.booleanValue() != null) return field.booleanValue();
        if (field.blobValue() != null)
            return field.blobValue().asByteArray();
        return null;
    }

    // ── Utilities ──────────────────────────────────────────────────

    private void cancelQuery(String queryId) {
        try {
            redshiftDataClient.cancelStatement(
                    CancelStatementRequest.builder()
                            .id(queryId)
                            .build());
            log.info("[Redshift-DataAPI] Cancelled timed-out " +
                    "query: {}", queryId);
        } catch (Exception e) {
            log.warn("[Redshift-DataAPI] Failed to cancel " +
                    "query {}: {}", queryId, e.getMessage());
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedshiftQueryException(
                    "Query polling interrupted.", e);
        }
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
