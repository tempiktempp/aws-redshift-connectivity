package com.edp.api.query;

import com.edp.api.definition.ColumnPreset;
import com.edp.api.definition.FilterTemplate;
import com.edp.api.definition.TableDefinition;
import com.edp.api.definition.TemplateParam;
import com.edp.api.definition.TemplateType;
import com.edp.api.exception.InvalidFilterParamException;
import com.edp.api.model.request.DataRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds safe parameterized SQL for STANDARD
 * and CTE template types.
 *
 * VIEW type is handled entirely by TemplateExecutor.
 *
 * Security guarantees:
 *   - Column identifiers from ColumnPreset only
 *   - Table/schema from TableDefinition only
 *   - Filter keys validated against allowedParams
 *   - All values bound as named parameters
 */
@Slf4j
@Component
public class QueryBuilder {

    public QueryComponents build(
            TableDefinition definition,
            FilterTemplate template,
            ColumnPreset columnPreset,
            DataRequest request) {

        Map<String, Object> parameters =
                new LinkedHashMap<>();

        // Apply template default params first
        if (template.getDefaultParams() != null) {
            parameters.putAll(
                    template.getDefaultParams());
        }

        // Resolve declared templateParams
        if (template.getTemplateParams() != null) {
            for (TemplateParam tp :
                    template.getTemplateParams()) {

                Object value;

                if (tp.getHardcodedValue() != null) {
                    // Hardcoded takes priority
                    value = tp.getHardcodedValue();

                } else if (tp.isFromEmployeeHeader()) {
                    // From X-Employee-Id header
                    value = request.getEmployeeId();

                } else {
                    // From URL query params
                    value = request.getFilterParams()
                            != null
                            ? request.getFilterParams()
                                     .get(tp.getParamName())
                            : null;
                }

                if (value != null) {
                    parameters.put(
                            tp.getParamName(), value);
                }
            }
        }

        // Validate and collect dynamic filter params
        if (request.getFilterParams() != null) {
            request.getFilterParams()
                    .forEach((key, value) -> {
                        // Skip already resolved params
                        if (parameters.containsKey(key)) {
                            return;
                        }
                        if (!template.getAllowedParams()
                                .contains(key)) {
                            throw new
                                InvalidFilterParamException(
                                    key,
                                    template.getName());
                        }
                        parameters.put(key, value);
                    });
        }

        String sql;

        if (request.getId() != null
                && !request.getId().isBlank()) {
            sql = buildIdQuery(
                    definition, columnPreset);
            parameters.put("id", request.getId());

        } else if (template.getTemplateType()
                == TemplateType.CTE) {
            sql = buildCteQuery(
                    definition, template, columnPreset);

        } else {
            sql = buildStandardQuery(
                    definition, template,
                    columnPreset, parameters);
        }

        log.debug("[QueryBuilder] SQL: {}", sql);
        log.debug("[QueryBuilder] Param keys: {}",
                parameters.keySet());

        return new QueryComponents(sql, parameters);
    }

    // ── Private builders ───────────────────────────────

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

        if (template.getSqlFragment() != null
                && !template.getSqlFragment().isBlank()) {
            sql.append(" WHERE ")
               .append(template.getSqlFragment());
            hasWhere = true;
        }

        for (String key : parameters.keySet()) {
            // Skip params already in sqlFragment
            if (template.getSqlFragment() != null
                    && template.getSqlFragment()
                               .contains(":" + key)) {
                continue;
            }
            if (!hasWhere) {
                sql.append(" WHERE ");
                hasWhere = true;
            } else {
                sql.append(" AND ");
            }
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

    public record QueryComponents(
            String sql,
            Map<String, Object> parameters) {}
                            }
