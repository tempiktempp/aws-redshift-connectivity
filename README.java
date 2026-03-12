package com.edp.api.facade;

import com.aws.utils.redshift.executor.RedshiftQueryExecutor;
import com.aws.utils.redshift.model.QueryRequest;
import com.edp.api.definition.ColumnPreset;
import com.edp.api.definition.FilterTemplate;
import com.edp.api.definition.TableDefinition;
import com.edp.api.exception.InvalidColumnPresetException;
import com.edp.api.exception.InvalidTemplateException;
import com.edp.api.model.request.DataRequest;
import com.edp.api.model.response.DataResponse;
import com.edp.api.query.QueryBuilder;
import com.edp.api.registry.TableRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full data access flow.
 *
 * Flow:
 *   1. Look up TableDefinition → 404 if unknown
 *   2. Resolve filter template → 400 if unknown
 *   3. Resolve column preset  → 400 if unknown
 *   4. Build safe SQL via QueryBuilder
 *   5. Execute via RedshiftQueryExecutor
 *   6. Return DataResponse
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataAccessFacade {

    private final TableRegistry tableRegistry;
    private final QueryBuilder queryBuilder;
    private final RedshiftQueryExecutor queryExecutor;

    public DataResponse fetchData(DataRequest request) {

        log.info("[DataAccessFacade] Request: " +
                "employee={}, schema={}, table={}, " +
                "template={}, columns={}, maxResults={}",
                request.getEmployeeId(),
                request.getSchemaName(),
                request.getTableName(),
                request.getTemplateName(),
                request.getColumnPreset(),
                request.getMaxResults());

        // Step 1 — resolve table definition
        // Unknown table → 404 before any SQL is built
        TableDefinition definition =
                tableRegistry.getDefinition(
                        request.getSchemaName(),
                        request.getTableName());

        // Step 2 — resolve filter template
        FilterTemplate template =
                resolveTemplate(request, definition);

        // Step 3 — resolve column preset
        ColumnPreset columnPreset =
                resolveColumnPreset(request, definition);

        // Step 4 — build safe SQL
        QueryBuilder.QueryComponents queryComponents =
                queryBuilder.build(
                        definition,
                        template,
                        columnPreset,
                        request);

        // Step 5 — execute
        QueryRequest.QueryRequestBuilder queryBuilder =
                QueryRequest.builder()
                        .sql(queryComponents.sql())
                        .maxResults(request.getMaxResults())
                        .queryLabel(
                                definition.getSchema() +
                                "-" +
                                definition.getTable() +
                                "-" +
                                template.getName());

        // Bind all parameters
        queryComponents.parameters()
                .forEach(queryBuilder::parameter);

        List<Map<String, Object>> rows =
                queryExecutor.queryForList(
                        queryBuilder.build());

        log.info("[DataAccessFacade] Rows returned: {}",
                rows.size());

        // Step 6 — wrap and return
        return DataResponse.builder()
                .schema(request.getSchemaName())
                .table(request.getTableName())
                .appliedTemplate(template.getName())
                .appliedColumnPreset(columnPreset.getName())
                .totalRows(rows.size())
                .rows(rows)
                .build();
    }

    // ── Private helpers ────────────────────────────────────────────

    private FilterTemplate resolveTemplate(
            DataRequest request,
            TableDefinition definition) {

        String templateName = request.getTemplateName() != null
                && !request.getTemplateName().isBlank()
                ? request.getTemplateName()
                : definition.getDefaultFilterTemplate();

        FilterTemplate template =
                definition.getFilterTemplates()
                        .get(templateName);

        if (template == null) {
            throw new InvalidTemplateException(
                    templateName,
                    request.getSchemaName(),
                    request.getTableName());
        }

        log.debug("[DataAccessFacade] Template resolved: {}",
                templateName);

        return template;
    }

    private ColumnPreset resolveColumnPreset(
            DataRequest request,
            TableDefinition definition) {

        String presetName = request.getColumnPreset() != null
                && !request.getColumnPreset().isBlank()
                ? request.getColumnPreset()
                : definition.getDefaultColumnPreset();

        ColumnPreset preset =
                definition.getColumnPresets()
                        .get(presetName);

        if (preset == null) {
            throw new InvalidColumnPresetException(
                    presetName,
                    request.getSchemaName(),
                    request.getTableName());
        }

        log.debug("[DataAccessFacade] Column preset: {}",
                presetName);

        return preset;
    }
            }
