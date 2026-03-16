package com.edp.api.definition.tables;

import com.edp.api.definition.ColumnPreset;
import com.edp.api.definition.EntitlementFragments;
import com.edp.api.definition.FilterTemplate;
import com.edp.api.definition.ParamSource;
import com.edp.api.definition.TableDefinition;
import com.edp.api.definition.TemplateParam;
import com.edp.api.definition.TemplateType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Table definition for crm.interaction.
 *
 * Filter templates:
 *   default          → STANDARD, open access
 *   my_team          → STANDARD, entitlement via
 *                      security.v_team_entitlement
 *   my_team_by_level → STANDARD, entitlement by level
 *   my_team_cte      → CTE, inline entitlement logic
 *   my_team_view     → VIEW, Redshift view
 *   my_team_proc     → PROC, stored procedure
 *
 * Column presets:
 *   default → core columns
 *   summary → minimal columns
 *   full    → all columns
 */
@Component
public class InteractionTableDefinition
        implements TableDefinition {

    private static final String ENTITLEMENT_CTE =
            """
            WITH team_employees AS (
                SELECT emp_id
                FROM dto.entitlement
                WHERE team_id = (
                    SELECT team_id
                    FROM dto.entitlement
                    WHERE emp_id = :employeeId
                    LIMIT 1
                )
            ),
            base_data AS (%s),
            entitled_data AS (
                SELECT bd.*
                FROM base_data bd
                WHERE bd.employee_id IN (
                    SELECT emp_id FROM team_employees
                )
            ),
            with_summary AS (
                SELECT
                    ed.*,
                    s.notes
                FROM entitled_data ed
                LEFT JOIN crm.interaction_summary s
                    ON s.interaction_id = ed.id
            ),
            with_attendees AS (
                SELECT
                    ws.*,
                    a.emp_id        AS attendee_emp_id,
                    a.attendee_name,
                    a.attendee_role
                FROM with_summary ws
                LEFT JOIN crm.interaction_attendee a
                    ON a.interaction_id = ws.id
            )
            SELECT * FROM with_attendees
            ORDER BY interaction_date DESC, id
            """;

    @Override
    public String getSchema() {
        return "crm";
    }

    @Override
    public String getTable() {
        return "interaction";
    }

    @Override
    public String getDefaultFilterTemplate() {
        return "default";
    }

    @Override
    public String getDefaultColumnPreset() {
        return "default";
    }

    @Override
    public Map<String, ColumnPreset> getColumnPresets() {
        Map<String, ColumnPreset> presets =
                new HashMap<>();

        presets.put("default", ColumnPreset.builder()
                .name("default")
                .column("id")
                .column("employee_id")
                .column("interaction_date")
                .column("interaction_type")
                .column("createddate")
                .build());

        presets.put("summary", ColumnPreset.builder()
                .name("summary")
                .column("id")
                .column("interaction_date")
                .column("interaction_type")
                .build());

        presets.put("full", ColumnPreset.builder()
                .name("full")
                .column("id")
                .column("employee_id")
                .column("interaction_date")
                .column("interaction_type")
                .column("createddate")
                .build());

        return Collections.unmodifiableMap(presets);
    }

    @Override
    public Map<String, FilterTemplate>
            getFilterTemplates() {
        Map<String, FilterTemplate> templates =
                new HashMap<>();

        // ── STANDARD — open access ─────────────────────
        templates.put("default", FilterTemplate.builder()
                .name("default")
                .templateType(TemplateType.STANDARD)
                .sqlFragment("")
                .allowedParam("interaction_type")
                .allowedParam("status")
                .build());

        // ── STANDARD — entitlement via security view ───
        templates.put("my_team", FilterTemplate.builder()
                .name("my_team")
                .templateType(TemplateType.STANDARD)
                .sqlFragment(
                    EntitlementFragments
                        .teamMemberFilter("employee_id"))
                .templateParam(TemplateParam.builder()
                    .paramName("employeeId")
                    .source(ParamSource
                            .FROM_EMPLOYEE_HEADER)
                    .build())
                .allowedParam("interaction_type")
                .allowedParam("status")
                .build());

        // ── STANDARD — entitlement by level ───────────
        templates.put("my_team_by_level",
                FilterTemplate.builder()
                    .name("my_team_by_level")
                    .templateType(TemplateType.STANDARD)
                    .sqlFragment(
                        EntitlementFragments
                            .teamMemberFilterByLevel(
                                "employee_id"))
                    .templateParam(TemplateParam.builder()
                        .paramName("employeeId")
                        .source(ParamSource
                                .FROM_EMPLOYEE_HEADER)
                        .build())
                    .templateParam(TemplateParam.builder()
                        .paramName("teamLevel")
                        .source(ParamSource
                                .FROM_REQUEST_PARAM)
                        .build())
                    .allowedParam("interaction_type")
                    .allowedParam("status")
                    .allowedParam("teamLevel")
                    .build());

        // ── CTE — inline entitlement logic ─────────────
        templates.put("my_team_cte",
                FilterTemplate.builder()
                    .name("my_team_cte")
                    .templateType(TemplateType.CTE)
                    .sqlFragment(ENTITLEMENT_CTE)
                    .allowedParam("interaction_type")
                    .allowedParam("status")
                    .build());

        // ── VIEW — Redshift view ───────────────────────
        templates.put("my_team_view",
                FilterTemplate.builder()
                    .name("my_team_view")
                    .templateType(TemplateType.VIEW)
                    .sqlFragment(
                        "SELECT * FROM " +
                        "crm.v_interaction_entitled " +
                        "WHERE employee_id = :employeeId")
                    .templateParam(TemplateParam.builder()
                        .paramName("employeeId")
                        .source(ParamSource
                                .FROM_EMPLOYEE_HEADER)
                        .build())
                    .allowedParam("interaction_type")
                    .allowedParam("status")
                    .build());

        // ── PROC — stored procedure ────────────────────
        templates.put("my_team_proc",
                FilterTemplate.builder()
                    .name("my_team_proc")
                    .templateType(TemplateType.PROC)
                    .sqlFragment(
                        "CALL crm.get_team_interactions" +
                        "(:employeeId)")
                    .procResultTable(
                        "temp_interaction_results")
                    .templateParam(TemplateParam.builder()
                        .paramName("employeeId")
                        .source(ParamSource
                                .FROM_EMPLOYEE_HEADER)
                        .build())
                    .build());

        return Collections.unmodifiableMap(templates);
    }
                }
