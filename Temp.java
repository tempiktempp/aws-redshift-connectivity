package com.aws.utils.redshift.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable value object containing the results of a Redshift query.
 *
 * Usage example:
 *
 *   QueryResult result = queryExecutor.executeQuery(request);
 *
 *   // Iterate rows
 *   result.getRows().forEach(row -> {
 *       String orderId = (String) row.get("order_id");
 *       Long   amount  = (Long)   row.get("amount");
 *   });
 *
 *   // Type-safe single value access
 *   Optional<String> status = result.getValue(0, "status", String.class);
 *
 *   // Single row lookup
 *   Map<String, Object> row = result.firstRow();
 *
 *   // Empty check
 *   if (result.isEmpty()) { ... }
 */
@Getter
@Builder
public class QueryResult {

    /**
     * Result rows. Each row is a Map of columnName -> value.
     *
     * Redshift types are mapped to Java types as follows:
     *   VARCHAR / CHAR / TEXT  ->  String
     *   INTEGER / BIGINT       ->  Long
     *   FLOAT / DOUBLE         ->  Double
     *   BOOLEAN                ->  Boolean
     *   NULL                   ->  null
     */
    private final List<Map<String, Object>> rows;

    /**
     * Column metadata — name and SQL type of each column
     * in the order they appear in the result set.
     */
    private final List<ColumnMetadata> columnMetadata;

    /**
     * Total number of rows returned in this result.
     */
    private final int totalRows;

    /**
     * Redshift Data API query execution ID.
     * Present only when strategy = DATA_API.
     * Null for JDBC strategy.
     * Use this to look up query history in AWS Console or CloudTrail.
     */
    private final String queryId;

    /**
     * Which connector strategy executed this query.
     * Either "DATA_API" or "JDBC".
     */
    private final String strategy;

    // ── Convenience methods ────────────────────────────────────────

    /**
     * Returns true if the result set contains no rows.
     */
    public boolean isEmpty() {
        return rows == null || rows.isEmpty();
    }

    /**
     * Returns the first row, or an empty map if no rows were returned.
     * Convenient for queries expected to return a single row.
     */
    public Map<String, Object> firstRow() {
        return isEmpty() ? Map.of() : rows.get(0);
    }

    /**
     * Type-safe value accessor for a specific row and column.
     *
     * @param rowIndex   zero-based row index
     * @param columnName column name as returned by Redshift
     * @param type       expected Java type
     * @return the value wrapped in Optional, or empty if null
     */
    public <T> Optional<T> getValue(int rowIndex,
                                    String columnName,
                                    Class<T> type) {
        if (isEmpty() || rowIndex >= rows.size()) {
            return Optional.empty();
        }
        Object value = rows.get(rowIndex).get(columnName);
        return Optional.ofNullable(type.cast(value));
    }

    // ── Nested value object ────────────────────────────────────────

    /**
     * Metadata for a single column in the result set.
     *
     * @param name     column name as returned by Redshift
     * @param typeName SQL type name e.g. "varchar", "int8", "bool"
     */
    public record ColumnMetadata(String name, String typeName) {}
}
