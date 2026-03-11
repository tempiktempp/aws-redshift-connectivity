package com.edp.api.service;

import com.aws.utils.redshift.executor.RedshiftQueryExecutor;
import com.aws.utils.redshift.model.QueryRequest;
import com.aws.utils.redshift.model.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service layer for fetching data from AWS Redshift.
 *
 * This class is the only place in edp-api-service that
 * interacts with RedshiftQueryExecutor directly.
 *
 * Responsibilities:
 *   - Validate and whitelist column/table identifiers
 *     before they are placed in SQL strings
 *   - Build QueryRequest objects with named parameters
 *   - Delegate execution to RedshiftQueryExecutor
 *
 * SQL Safety rules enforced here:
 *   - Filter VALUES always passed as named parameters
 *     e.g. WHERE status = :status
 *   - Column and table identifiers validated against
 *     a whitelist before being placed in SQL
 *   - Raw user input NEVER interpolated into SQL strings
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedshiftDataService {

    private final RedshiftQueryExecutor queryExecutor;

    // ── Whitelists ─────────────────────────────────────────────────
    // Column and table identifiers must come from these sets only.
    // Never allow user input to define SQL identifiers directly.

    private static final Set<String> ALLOWED_TABLES = Set.of(
            "users",
            "orders",
            "products"
            // add more tables here as needed
    );

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "id",
            "name",
            "status",
            "amount",
            "created_at",
            "updated_at"
            // add more columns here as needed
    );

    // ── Public API ─────────────────────────────────────────────────

    /**
     * Fetches all rows from a table with an optional status filter.
     *
     * Table name is whitelisted — cannot be injected by a caller.
     * Status value is a named parameter — cannot be injected either.
     *
     * @param tableName name of the Redshift table to query
     * @param status    optional status filter — null means no filter
     * @param maxResults max rows to return
     * @return list of rows as column-name to value maps
     */
    public List<Map<String, Object>> fetchTableData(
            String tableName,
            String status,
            int maxResults) {

        // Validate table identifier against whitelist
        validateTableName(tableName);

        // Build SQL — identifier comes from whitelist (safe)
        // value comes from named parameter (safe)
        String sql = status != null
                ? "SELECT * FROM " + tableName +
                  " WHERE status = :status"
                : "SELECT * FROM " + tableName;

        QueryRequest.QueryRequestBuilder builder =
                QueryRequest.builder()
                        .sql(sql)
                        .maxResults(maxResults)
                        .queryLabel("fetch-" + tableName);

        if (status != null) {
            builder.parameter("status", status);
        }

        return queryExecutor.queryForList(builder.build());
    }

    /**
     * Fetches a single row by ID from a given table.
     *
     * @param tableName name of the Redshift table
     * @param id        the row ID to look up
     * @return single row as a column-name to value map,
     *         or empty map if not found
     */
    public Map<String, Object> fetchById(
            String tableName,
            Object id) {

        validateTableName(tableName);

        QueryRequest request = QueryRequest.builder()
                .sql("SELECT * FROM " + tableName +
                     " WHERE id = :id")
                .parameter("id", id)
                .maxResults(1)
                .queryLabel("fetch-by-id-" + tableName)
                .build();

        return queryExecutor.queryForSingleRow(request);
    }

    /**
     * Executes a custom query with caller-supplied parameters.
     *
     * The SQL must only reference whitelisted columns.
     * This method validates all parameter keys against the
     * column whitelist before executing.
     *
     * @param sql        parameterized SQL with named placeholders
     * @param parameters named parameter map
     * @param maxResults max rows to return
     * @return full QueryResult including metadata
     */
    public QueryResult executeCustomQuery(
            String sql,
            Map<String, Object> parameters,
            int maxResults) {

        // Validate all parameter keys against column whitelist
        if (parameters != null) {
            parameters.keySet().forEach(this::validateColumnName);
        }

        QueryRequest.QueryRequestBuilder builder =
                QueryRequest.builder()
                        .sql(sql)
                        .maxResults(maxResults)
                        .queryLabel("custom-query");

        if (parameters != null) {
            parameters.forEach(builder::parameter);
        }

        return queryExecutor.executeQuery(builder.build());
    }

    // ── Private validation ─────────────────────────────────────────

    private void validateTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name must not be null or blank.");
        }
        if (!ALLOWED_TABLES.contains(tableName.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Table not allowed: " + tableName +
                    ". Allowed tables: " + ALLOWED_TABLES);
        }
    }

    private void validateColumnName(String columnName) {
        if (!ALLOWED_COLUMNS.contains(columnName.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Column not allowed: " + columnName +
                    ". Allowed columns: " + ALLOWED_COLUMNS);
        }
    }
}
