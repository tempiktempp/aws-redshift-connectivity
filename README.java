package com.edp.api.query;

import com.edp.api.definition.ColumnPreset;
import com.edp.api.definition.FilterTemplate;
import com.edp.api.definition.TableDefinition;
import com.edp.api.exception.InvalidFilterParamException;
import com.edp.api.model.request.DataRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds safe parameterized SQL from a TableDefinition,
 * FilterTemplate and DataRequest.
 *
 * Security guarantees:
 *   - Column identifiers come ONLY from ColumnPreset
 *   - Table/schema identifiers come ONLY from TableDefinition
 *   - Filter keys validated against template allowedParams
 *   - All filter values bound as named parameters
 *   - Unknown filter params → 400 Bad Request
 *   - Internal params (employeeId) never appear in WHERE
 */
@Slf4j
@Component
public class QueryBuilder {

    /**
     * Internal params bound as named parameters
     * but never placed in WHERE clause as filters.
     * Used by CTE templates internally.
     */
    private static final Set<String> INTERNAL_PARAMS =
            Set.of("employeeId");

    /**
     * Builds SQL and parameter map for the given request.
     *
     * Flow:
     *   1. Apply template default params
     *   2. Validate and collect dynamic filter params
     *   3. Add employeeId for entitlement templates
     *   4. Build SQL based on template type
     *      - ID query    → simple SELECT WHERE id = :id
     *      - CTE         → wrap base SELECT in CTE
     *      - Standard    → SELECT with optional WHERE
     *
     * @return QueryComponents with final SQL and parameters
     */
    public QueryComponents build(
            TableDefinition definition,
            FilterTemplate template,
            ColumnPreset columnPreset,
            DataRequest request) {

        // Collect all parameters here —
        // template defaults first, then caller params
        Map<String, Object> parameters =
                new LinkedHashMap<>();

        // Step 1 — apply template default params
        if (template.getDefaultParams() != null) {
            parameters.putAll(
                    template.getDefaultParams());
        }

        // Step 2 — validate and collect dynamic params
        // Unknown params → 400 Bad Request
        if (request.getFilterParams() != null) {
            request.getFilterParams()
                    .forEach((key, value) -> {
                        if (!template.getAllowedParams()
                                .contains(key)) {
                            throw new InvalidFilterParamException(
                                    key,
                                    template.getName());
                        }
                        // Caller param overrides default
                        parameters.put(key, value);
                    });
        }

        // Step 3 — always include employeeId
        // Needed for entitlement CTE templates.
        // Excluded from WHERE clause by INTERNAL_PARAMS.
        parameters.put("employeeId",
                request.getEmployeeId());

        // Step 4 — build SQL based on template type
        String sql;

        if (request.getId() != null
                && !request.getId().isBlank()) {
            // Single row by ID
            sql = buildIdQuery(
                    definition, columnPreset);
            parameters.put("id", request.getId());

        } else if (template.isCte()) {
            // CTE template — wraps base SELECT entirely
            sql = buildCteQuery(
                    definition, template, columnPreset);

        } else {
            // Standard SELECT with optional WHERE
            sql = buildStandardQuery(
                    definition, template,
                    columnPreset, parameters);
        }

        log.debug("[QueryBuilder] SQL built: {}", sql);
        log.debug("[QueryBuilder] Param keys: {}",
                parameters.keySet());

        return new QueryComponents(sql, parameters);
    }

    // ── Private builders ───────────────────────────────────────────

    /**
     * Simple SELECT WHERE id = :id
     * Used for single row lookups.
     */
    private String buildIdQuery(
            TableDefinition definition,
            ColumnPreset columnPreset) {
        return "SELECT " +
                buildSelectClause(columnPreset) +
                " FROM " +
                definition.getSchema() + "." +
                definition.getTable() +
                " WHERE id = :id";
    }

    /**
     * CTE template — injects base SELECT into
     * the %s placeholder in the template's sqlFragment.
     * Used for entitlement and other complex filters.
     */
    private String buildCteQuery(
            TableDefinition definition,
            FilterTemplate template,
            ColumnPreset columnPreset) {

        String baseSelect =
                "SELECT " +
                buildSelectClause(columnPreset) +
                " FROM " +
                definition.getSchema() + "." +
                definition.getTable();

        return String.format(
                template.getSqlFragment(), baseSelect);
    }

    /**
     * Standard SELECT with optional WHERE clause.
     *
     * WHERE is built from:
     *   1. Fixed template sqlFragment (if any)
     *   2. Dynamic caller params appended with AND
     *
     * INTERNAL_PARAMS are excluded from WHERE —
     * they are bound as parameters but are only
     * meaningful inside CTE templates.
     */
    private String buildStandardQuery(
            TableDefinition definition,
            FilterTemplate template,
            ColumnPreset columnPreset,
            Map<String, Object> parameters) {

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT ")
           .append(buildSelectClause(columnPreset))
           .append(" FROM ")
           .append(definition.getSchema())
           .append(".")
           .append(definition.getTable());

        boolean hasWhere = false;

        // Fixed template fragment first
        if (template.getSqlFragment() != null
                && !template.getSqlFragment().isBlank()) {
            sql.append(" WHERE ")
               .append(template.getSqlFragment());
            hasWhere = true;
        }

        // Dynamic caller params appended with AND
        for (String key : parameters.keySet()) {

            // Skip internal params — not data filters
            // e.g. employeeId is for CTE use only
            if (INTERNAL_PARAMS.contains(key)) {
                continue;
            }

            if (!hasWhere) {
                sql.append(" WHERE ");
                hasWhere = true;
            } else {
                sql.append(" AND ");
            }

            // Key from allowedParams whitelist — safe
            // Value bound as named param — injection safe
            sql.append(key)
               .append(" = :")
               .append(key);
        }

        return sql.toString();
    }

    /**
     * Builds SELECT clause from column preset.
     * Columns sorted for consistent output.
     * Never uses SELECT * — always explicit columns.
     */
    private String buildSelectClause(
            ColumnPreset columnPreset) {
        return columnPreset.getColumns()
                .stream()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    // ── Value object ───────────────────────────────────────────────

    /**
     * Holds the final SQL and bound parameters.
     * Passed from QueryBuilder to DataAccessFacade.
     */
    public record QueryComponents(
            String sql,
            Map<String, Object> parameters) {}
}
