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
 */
@Slf4j
@Component
public class QueryBuilder {

    /**
     * Builds SQL and parameter map for the given request.
     *
     * Flow:
     *   1. Build SELECT clause from column preset
     *   2. Build FROM clause from table definition
     *   3. Apply filter template (CTE or WHERE)
     *   4. Append dynamic params as AND conditions
     *   5. Apply ID filter if present
     *
     * @return QueryComponents with final SQL and parameters
     */
    public QueryComponents build(
            TableDefinition definition,
            FilterTemplate template,
            ColumnPreset columnPreset,
            DataRequest request) {

        // All parameters collected here —
        // template defaults first, then caller params
        Map<String, Object> parameters =
                new LinkedHashMap<>();

        // Apply template default params first
        if (template.getDefaultParams() != null) {
            parameters.putAll(
                    template.getDefaultParams());
        }

        // Validate and collect dynamic filter params
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

        // Add employeeId for entitlement templates
        parameters.put("employeeId",
                request.getEmployeeId());

        String sql;

        if (request.getId() != null
                && !request.getId().isBlank()) {
            // Single row by ID — simple SELECT
            sql = buildIdQuery(
                    definition, columnPreset, request);
            parameters.put("id", request.getId());

        } else if (template.isCte()) {
            // CTE template wraps base SELECT entirely
            sql = buildCteQuery(
                    definition, template, columnPreset,
                    parameters);

        } else {
            // Standard SELECT with optional WHERE
            sql = buildStandardQuery(
                    definition, template, columnPreset,
                    parameters);
        }

        log.debug("[QueryBuilder] SQL: {}", sql);
        log.debug("[QueryBuilder] Param keys: {}",
                parameters.keySet());

        return new QueryComponents(sql, parameters);
    }

    // ── Private builders ───────────────────────────────────────────

    private String buildIdQuery(
            TableDefinition definition,
            ColumnPreset columnPreset,
            DataRequest request) {
        return "SELECT " +
                buildSelectClause(columnPreset) +
                " FROM " +
                definition.getSchema() + "." +
                definition.getTable() +
                " WHERE id = :id";
    }

    private String buildCteQuery(
            TableDefinition definition,
            FilterTemplate template,
            ColumnPreset columnPreset,
            Map<String, Object> parameters) {

        // Base SELECT injected into CTE %s placeholder
        String baseSelect =
                "SELECT " +
                buildSelectClause(columnPreset) +
                " FROM " +
                definition.getSchema() + "." +
                definition.getTable();

        return String.format(
                template.getSqlFragment(), baseSelect);
    }

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

        // Fixed template fragment first
        boolean hasWhere = false;
        if (template.getSqlFragment() != null
                && !template.getSqlFragment().isBlank()) {
            sql.append(" WHERE ")
               .append(template.getSqlFragment());
            hasWhere = true;
        }

        // Dynamic caller params appended with AND
        for (String key : parameters.keySet()) {
            // Skip internal params not meant for WHERE
            if (key.equals("employeeId")) continue;

            if (!hasWhere) {
                sql.append(" WHERE ");
                hasWhere = true;
            } else {
                sql.append(" AND ");
            }
            // Key from whitelist — safe as identifier
            // Value bound as named param — injection safe
            sql.append(key)
               .append(" = :")
               .append(key);
        }

        return sql.toString();
    }

    private String buildSelectClause(
            ColumnPreset columnPreset) {
        return columnPreset.getColumns()
                .stream()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    // ── Value object ───────────────────────────────────────────────

    /**
     * Holds the built SQL and bound parameters.
     */
    public record QueryComponents(
            String sql,
            Map<String, Object> parameters) {}
            }
