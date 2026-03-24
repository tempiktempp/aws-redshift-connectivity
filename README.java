package com.edp.api.service;

import com.aws.utils.redshift.executor.RedshiftQueryExecutor;
import com.aws.utils.redshift.model.QueryRequest;
import com.edp.api.model.request.InteractionFilterRequest;
import com.edp.api.model.response.InteractionResponse;
import com.edp.api.query.InteractionQueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for interaction data queries.
 *
 * Orchestrates:
 *   1. Build SQL via InteractionQueryBuilder
 *   2. Execute via RedshiftQueryExecutor
 *   3. Return InteractionResponse
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionService {

    private final RedshiftQueryExecutor queryExecutor;
    private final InteractionQueryBuilder queryBuilder;

    /**
     * Fetches interactions based on filter request
     * and employee entitlement.
     *
     * @param request    filter params from JSON body
     * @param employeeId from X-Employee-Id header
     * @param maxResults max rows to return
     * @return InteractionResponse with rows
     */
    public InteractionResponse fetchInteractions(
            InteractionFilterRequest request,
            String employeeId,
            int maxResults) {

        log.info("[InteractionService] Fetching. " +
                "employee={}, viewType={}, " +
                "clientId={}, maxResults={}",
                employeeId,
                request.getViewType(),
                request.getClientId(),
                maxResults);

        // Build SQL and params
        InteractionQueryBuilder.QueryComponents
                queryComponents = queryBuilder.build(
                        request, employeeId, maxResults);

        // Execute query
        QueryRequest.QueryRequestBuilder builder =
                QueryRequest.builder()
                        .sql(queryComponents.sql())
                        .maxResults(maxResults)
                        .queryLabel(
                                "interaction-" +
                                request.getViewType()
                                       .toLowerCase()
                                       .replace(" ", "-"));

        queryComponents.parameters()
                .forEach(builder::parameter);

        List<Map<String, Object>> rows =
                queryExecutor.queryForList(
                        builder.build());

        log.info("[InteractionService] Rows returned: {}",
                rows.size());

        return InteractionResponse.builder()
                .viewType(request.getViewType())
                .totalRows(rows.size())
                .rows(rows)
                .build();
    }
}
