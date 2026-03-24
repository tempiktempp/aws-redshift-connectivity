package com.edp.api.query;

import com.edp.api.model.request.InteractionFilterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds safe parameterized SQL for interaction queries.
 *
 * Two query strategies based on viewType:
 *
 *   My team:
 *     SELECT * FROM security.v_interaction_complete
 *     WHERE source_employee_id IN (
 *         SELECT team_member_id
 *         FROM security.v_team_entitlements
 *         WHERE source_employee_id = :employeeId
 *     )
 *     AND {optional filters}
 *
 *   My interaction:
 *     SELECT * FROM security.v_interaction_complete
 *     WHERE source_employee_id = :employeeId
 *     AND {optional filters}
 *
 * Security guarantees:
 *   - viewType validated against allowed values
 *   - All filter values bound as named parameters
 *   - Column names hardcoded — never from user input
 */
@Slf4j
@Component
public class InteractionQueryBuilder {

    private static final String VIEW =
            "security.v_interaction_complete";

    private static final String ENTITLEMENT_VIEW =
            "security.v_team_entitlements";

    private static final String VIEW_TYPE_MY_TEAM =
            "My team";

    private static final String VIEW_TYPE_MY_INTERACTION =
            "My interaction";

    /**
     * Builds SQL and parameters for interaction query.
     *
     * @param request     filter params from JSON body
     * @param employeeId  from X-Employee-Id header
     * @param maxResults  max rows to return
     * @return QueryComponents with SQL and bound params
     */
    public QueryComponents build(
            InteractionFilterRequest request,
            String employeeId,
            int maxResults) {

        validateViewType(request.getViewType());

        Map<String, Object> params =
                new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT * FROM ")
           .append(VIEW)
           .append(" WHERE ");

        // ── Entitlement filter ─────────────────────────
        if (VIEW_TYPE_MY_TEAM.equalsIgnoreCase(
                request.getViewType())) {

            // Team entitlement — nested subquery
            sql.append("source_employee_id IN (")
               .append("SELECT team_member_id ")
               .append("FROM ")
               .append(ENTITLEMENT_VIEW)
               .append(" WHERE source_employee_id ")
               .append("= :employeeId")
               .append(")");

        } else {

            // My interaction — direct employee filter
            sql.append("source_employee_id ")
               .append("= :employeeId");
        }

        params.put("employeeId", employeeId);

        // ── Optional filters ───────────────────────────
        // Each filter only added if value is present
        // All values bound as named params — never
        // interpolated into SQL string

        if (hasValue(request.getClientId())) {
            sql.append(" AND accountid = :accountId");
            params.put("accountId", request.getClientId());
        }

        if (hasValue(request.getTypeDesc())) {
            sql.append(
                " AND interactiontype = :interactionType");
            params.put("interactionType",
                    request.getTypeDesc());
        }

        if (hasValue(request.getDateFrom())) {
            sql.append(" AND starttime >= :dateFrom");
            params.put("dateFrom", request.getDateFrom());
        }

        if (hasValue(request.getDateTo())) {
            sql.append(" AND endtime <= :dateTo");
            params.put("dateTo", request.getDateTo());
        }

        log.debug("[InteractionQueryBuilder] SQL: {}",
                sql);
        log.debug("[InteractionQueryBuilder] " +
                "Param keys: {}", params.keySet());

        return new QueryComponents(
                sql.toString(), params);
    }

    // ── Private helpers ────────────────────────────────

    private void validateViewType(String viewType) {
        if (!VIEW_TYPE_MY_TEAM.equalsIgnoreCase(viewType)
                && !VIEW_TYPE_MY_INTERACTION
                        .equalsIgnoreCase(viewType)) {
            throw new IllegalArgumentException(
                    "Invalid viewType: '" + viewType +
                    "'. Allowed values: '" +
                    VIEW_TYPE_MY_TEAM + "', '" +
                    VIEW_TYPE_MY_INTERACTION + "'");
        }
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    // ── Value object ───────────────────────────────────

    public record QueryComponents(
            String sql,
            Map<String, Object> parameters) {}
}
