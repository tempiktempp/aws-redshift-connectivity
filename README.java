package com.edp.api.facade;

import com.edp.api.definition.ColumnPreset;
import com.edp.api.definition.FilterTemplate;
import com.edp.api.definition.TableDefinition;
import com.edp.api.definition.TemplateType;
import com.edp.api.exception.InvalidColumnPresetException;
import com.edp.api.exception.InvalidTemplateException;
import com.edp.api.model.request.DataRequest;
import com.edp.api.model.response.DataResponse;
import com.edp.api.query.QueryBuilder;
import com.edp.api.query.TemplateExecutor;
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
 *   1. Look up TableDefinition  → 404 if unknown
 *   2. Resolve FilterTemplate   → 400 if unknown
 *   3. Resolve ColumnPreset     → 400 if unknown
 *                                 skipped for VIEW
 *   4. Build SQL via QueryBuilder
 *                                 skipped for VIEW
 *   5. Execute via TemplateExecutor
 *   6. Return DataResponse
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataAccessFacade {

    private final TableRegistry tableRegistry;
    private final QueryBuilder queryBuilder;
    private final TemplateExecutor templateExecutor;

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
        TableDefinition definition =
                tableRegistry.getDefinition(
                        request.getSchemaName(),
                        request.getTableName());

        // Step 2 — resolve filter template
        FilterTemplate template =
                resolveTemplate(request, definition);

        log.info("[DataAccessFacade] TemplateType: {}",
                template.getTemplateType());

        List<Map<String, Object>> rows;

        if (template.getTemplateType()
                == TemplateType.VIEW) {

            // VIEW — TemplateExecutor handles everything
            // Column presets not applied
            rows = templateExecutor.execute(
                    request, template, null);

        } else {

            // STANDARD / CTE — resolve column preset
            // and build SQL via QueryBuilder
            ColumnPreset columnPreset =
                    resolveColumnPreset(
                            request, definition);

            QueryBuilder.QueryComponents queryComponents =
                    queryBuilder.build(
                            definition,
                            template,
                            columnPreset,
                            request);

            rows = templateExecutor.execute(
                    request, template, queryComponents);
        }

        log.info("[DataAccessFacade] Rows returned: {}",
                rows.size());

        return DataResponse.builder()
                .schema(request.getSchemaName())
                .table(request.getTableName())
                .appliedTemplate(template.getName())
                .appliedColumnPreset(
                        request.getColumnPreset())
                .totalRows(rows.size())
                .rows(rows)
                .build();
    }

    // ── Private helpers ────────────────────────────────

    private FilterTemplate resolveTemplate(
            DataRequest request,
            TableDefinition definition) {

        String templateName =
                request.getTemplateName() != null
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

        return template;
    }

    private ColumnPreset resolveColumnPreset(
            DataRequest request,
            TableDefinition definition) {

        String presetName =
                request.getColumnPreset() != null
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

        return preset;
    }
            }
