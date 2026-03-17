package com.edp.api.query;

import com.aws.utils.redshift.executor.RedshiftQueryExecutor;
import com.aws.utils.redshift.model.QueryRequest;
import com.edp.api.definition.FilterTemplate;
import com.edp.api.definition.TemplateParam;
import com.edp.api.exception.InvalidFilterParamException;
import com.edp.api.model.request.DataRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes a query based on its TemplateType.
 *
 * Routes to correct execution path:
 *   STANDARD / CTE → uses QueryComponents from QueryBuilder
 *   VIEW           → executes sqlFragment as-is
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateExecutor {

    private final RedshiftQueryExecutor queryExecutor;

    public List<Map<String, Object>> execute(
            DataRequest request,
            FilterTemplate template,
            QueryBuilder.QueryComponents queryComponents) {

        return switch (template.getTemplateType()) {
            case STANDARD, CTE ->
                    executeStandardOrCte(
                            request, template,
                            queryComponents);
            case VIEW ->
                    executeView(request, template);
        };
    }

    // ── STANDARD / CTE ────────────────────────────────

    private List<Map<String, Object>> executeStandardOrCte(
            DataRequest request,
            FilterTemplate template,
            QueryBuilder.QueryComponents queryComponents) {

        log.debug("[TemplateExecutor] Executing {}: {}",
                template.getTemplateType(),
                template.getName());

        QueryRequest.QueryRequestBuilder builder =
                QueryRequest.builder()
                        .sql(queryComponents.sql())
                        .maxResults(request.getMaxResults())
                        .queryLabel(buildLabel(
                                request, template));

        queryComponents.parameters()
                .forEach(builder::parameter);

        return queryExecutor.queryForList(builder.build());
    }

    // ── VIEW ──────────────────────────────────────────

    private List<Map<String, Object>> executeView(
            DataRequest request,
            FilterTemplate template) {

        log.debug("[TemplateExecutor] Executing VIEW: {}",
                template.getName());

        Map<String, Object> params =
                resolveTemplateParams(request, template);

        StringBuilder sql = new StringBuilder(
                template.getSqlFragment());

        appendDynamicParams(
                sql, params, request, template);

        QueryRequest.QueryRequestBuilder builder =
                QueryRequest.builder()
                        .sql(sql.toString())
                        .maxResults(request.getMaxResults())
                        .queryLabel(buildLabel(
                                request, template));

        params.forEach(builder::parameter);

        return queryExecutor.queryForList(builder.build());
    }

    // ── Param resolution ──────────────────────────────

    private Map<String, Object> resolveTemplateParams(
            DataRequest request,
            FilterTemplate template) {

        Map<String, Object> params =
                new LinkedHashMap<>();

        if (template.getDefaultParams() != null) {
            params.putAll(template.getDefaultParams());
        }

        if (template.getTemplateParams() != null) {
            for (TemplateParam tp :
                    template.getTemplateParams()) {

                Object value;

                if (tp.getHardcodedValue() != null) {
                    value = tp.getHardcodedValue();

                } else if (tp.isFromEmployeeHeader()) {
                    // employeeId from X-Employee-Id header
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
                    params.put(
                            tp.getParamName(), value);
                }

                log.debug("[TemplateExecutor] Param " +
                        "'{}' resolved",
                        tp.getParamName());
            }
        }

        return params;
    }

    private void appendDynamicParams(
            StringBuilder sql,
            Map<String, Object> params,
            DataRequest request,
            FilterTemplate template) {

        if (request.getFilterParams() == null
                || request.getFilterParams().isEmpty()) {
            return;
        }

        request.getFilterParams().forEach((key, value) -> {
            if (params.containsKey(key)) return;

            if (!template.getAllowedParams()
                    .contains(key)) {
                throw new InvalidFilterParamException(
                        key, template.getName());
            }

            sql.append(" AND ")
               .append(key)
               .append(" = :")
               .append(key);

            params.put(key, value);
        });
    }

    private String buildLabel(
            DataRequest request,
            FilterTemplate template) {
        return request.getSchemaName() + "-" +
               request.getTableName() + "-" +
               template.getName();
    }
}
