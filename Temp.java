package com.aws.utils.redshift.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Map;

/**
 * Immutable value object representing a query to execute against Redshift.
 *
 * Usage example:
 *
 *   QueryRequest request = QueryRequest.builder()
 *       .sql("SELECT order_id, amount FROM fact_orders WHERE status = :status")
 *       .parameter("status", "OPEN")
 *       .maxResults(100)
 *       .queryLabel("open-orders")
 *       .build();
 *
 * SQL Safety Rules:
 *   - Filter VALUES must always be passed as named parameters (:paramName)
 *     never interpolated into the SQL string directly
 *   - Column and table names must come from a validated whitelist
 *     in the calling service — never from raw user input
 */
@Getter
@Builder
public class QueryRequest {

    /**
     * The SQL to execute.
     * Use named parameters for all dynamic values e.g. WHERE status = :status
     */
    private final String sql;

    /**
     * Named parameters to bind into the SQL.
     * Key   = parameter name (without the colon)
     * Value = parameter value
     *
     * @Singular allows calling .parameter("key", value)
     * multiple times in the builder instead of building a map manually.
     */
    @Singular("parameter")
    private final Map<String, Object> parameters;

    /**
     * Maximum rows to return.
     * If 0 or not set, the global redshift.query.max-results-per-page is used.
     * Cannot exceed the global cap regardless of what is set here.
     */
    @Builder.Default
    private final int maxResults = 0;

    /**
     * Query timeout in seconds.
     * If 0 or not set, the global redshift.query.timeout-seconds is used.
     */
    @Builder.Default
    private final int timeoutSeconds = 0;

    /**
     * Optional label for logging and monitoring.
     * Appears in log lines so you can identify queries without
     * exposing the full SQL.
     * Example: "orders-by-status", "customer-lookup"
     */
    private final String queryLabel;
}
