package com.aws.utils.redshift.executor;

import com.aws.utils.redshift.config.RedshiftProperties;
import com.aws.utils.redshift.connection.RedshiftConnector;
import com.aws.utils.redshift.exception.RedshiftQueryException;
import com.aws.utils.redshift.model.QueryRequest;
import com.aws.utils.redshift.model.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The primary public API of aws-redshift-utils.
 *
 * This is the ONLY class consuming projects need to inject.
 * It wraps the connector strategy and adds cross-cutting
 * concerns transparently:
 *
 *   - Input validation
 *     Rejects null requests and blank SQL immediately
 *     before anything hits the network.
 *
 *   - Execution timing
 *     Logs query duration in milliseconds for every execution.
 *     Useful for identifying slow queries in production.
 *
 *   - Correlation ID
 *     Injects a unique redshiftQueryId into MDC on every
 *     execution so all log lines for one query are traceable
 *     across the connector and polling layers.
 *
 *   - Strategy transparency
 *     Callers never know whether DATA_API or JDBC is active.
 *     Switching strategies requires zero changes in consuming code.
 *
 *
 * Usage in a consuming service:
 *
 *   @Service
 *   @RequiredArgsConstructor
 *   public class OrderService {
 *
 *       private final RedshiftQueryExecutor queryExecutor;
 *
 *       public List<Map<String, Object>> getOpenOrders(String status) {
 *           QueryRequest request = QueryRequest.builder()
 *               .sql("SELECT order_id, amount " +
 *                    "FROM fact_orders " +
 *                    "WHERE status = :status")
 *               .parameter("status", status)
 *               .queryLabel("open-orders")
 *               .maxResults(100)
 *               .build();
 *
 *           return queryExecutor.queryForList(request);
 *       }
 *   }
 */
@Slf4j
@RequiredArgsConstructor
public class RedshiftQueryExecutor {

    private final RedshiftConnector connector;
    private final RedshiftProperties props;

    private static final String MDC_QUERY_ID    = "redshiftQueryId";
    private static final String MDC_QUERY_LABEL = "redshiftQueryLabel";

    // ── Core execution ─────────────────────────────────────────────

    /**
     * Executes a SQL query and returns the full result set.
     *
     * @param request the query — SQL, parameters and options
     * @return QueryResult containing rows, metadata and execution info
     * @throws IllegalArgumentException if request or SQL is null/blank
     * @throws RedshiftQueryException   if the query fails at Redshift level
     */
    public QueryResult executeQuery(QueryRequest request) {
        validateRequest(request);

        String executionId = UUID.randomUUID().toString();
        String label = request.getQueryLabel() != null
                ? request.getQueryLabel()
                : "unnamed";

        MDC.put(MDC_QUERY_ID, executionId);
        MDC.put(MDC_QUERY_LABEL, label);

        Instant start = Instant.now();

        try {
            log.info("[RedshiftExecutor] Starting query. " +
                            "label={}, strategy={}",
                    label,
                    connector.getStrategyName());

            QueryResult result = connector.executeQuery(request);

            long durationMs = Duration
                    .between(start, Instant.now())
                    .toMillis();

            log.info("[RedshiftExecutor] Query completed. " +
                            "label={}, rows={}, duration_ms={}",
                    label,
                    result.getTotalRows(),
                    durationMs);

            return result;

        } catch (RedshiftQueryException e) {
            long durationMs = Duration
                    .between(start, Instant.now())
                    .toMillis();

            log.error("[RedshiftExecutor] Query failed. " +
                            "label={}, duration_ms={}, error={}",
                    label,
                    durationMs,
                    e.getMessage());

            // Re-throw as-is — don't wrap again,
            // preserves the original exception type
            // (RedshiftTimeoutException stays as timeout)
            throw e;

        } finally {
            // Always clean up MDC — thread-local leak otherwise
            MDC.remove(MDC_QUERY_ID);
            MDC.remove(MDC_QUERY_LABEL);
        }
    }

    // ── Convenience methods ────────────────────────────────────────

    /**
     * Executes a query and returns only the rows list.
     * Use when you don't need metadata or the queryId.
     *
     * Example:
     *   List<Map<String, Object>> rows = queryExecutor.queryForList(
     *       QueryRequest.builder()
     *           .sql("SELECT * FROM fact_orders WHERE status = :status")
     *           .parameter("status", "OPEN")
     *           .build()
     *   );
     */
    public List<Map<String, Object>> queryForList(
            QueryRequest request) {
        return executeQuery(request).getRows();
    }

    /**
     * Executes a query and returns the first row only.
     * Returns an empty map if no rows are found.
     *
     * Useful for ID-based lookups expected to return 0 or 1 row.
     *
     * Example:
     *   Map<String, Object> order = queryExecutor.queryForSingleRow(
     *       QueryRequest.builder()
     *           .sql("SELECT * FROM fact_orders " +
     *                "WHERE order_id = :orderId")
     *           .parameter("orderId", "ORD-001")
     *           .maxResults(1)
     *           .build()
     *   );
     *
     *   String status = (String) order.get("status");
     */
    public Map<String, Object> queryForSingleRow(
            QueryRequest request) {
        return executeQuery(request).firstRow();
    }

    /**
     * Returns the name of the active connector strategy.
     * Useful for diagnostic endpoints or admin dashboards.
     *
     * @return "DATA_API" or "JDBC"
     */
    public String getActiveStrategy() {
        return connector.getStrategyName();
    }

    // ── Private helpers ────────────────────────────────────────────

    private void validateRequest(QueryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "QueryRequest must not be null.");
        }
        if (request.getSql() == null
                || request.getSql().isBlank()) {
            throw new IllegalArgumentException(
                    "QueryRequest.sql must not be null or blank.");
        }
    }
}
```

---

### ✅ What you should see when Step 7 is done
```
com/aws/utils/redshift/
└── executor/
    └── RedshiftQueryExecutor.java
```

Red errors in `RedshiftAutoConfiguration` should now be down to only these two remaining:
```
RedshiftHealthIndicator     ← Step 8
CredentialsProviderFactory  ← Step 8
SecretsManagerUtil          ← Step 8
