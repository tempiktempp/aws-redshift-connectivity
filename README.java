package com.edp.api.definition.tables;

import com.edp.api.definition.ColumnPreset;
import com.edp.api.definition.EntitlementFragments;
import com.edp.api.definition.FilterTemplate;
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
 * To switch default filter template:
 *   Change DEFAULT_FILTER_TEMPLATE to any template name
 *
 * To switch default column preset:
 *   Change DEFAULT_COLUMN_PRESET to any preset name
 *
 * Filter templates:
 *   default          → STANDARD, open access
 *   my_team          → STANDARD, entitlement via
 *                      security.v_team_entitlement
 *   my_team_by_level → STANDARD, entitlement by level
 *   my_team_cte      → CTE, inline entitlement logic
 *   my_team_view     → VIEW, Redshift view (DEFAULT)
 *
 * Column presets:
 *   default → core columns (DEFAULT)
 *   summary → minimal columns
 *   full    → SELECT * all columns
 */
@Component
public class InteractionTableDefinition
        implements TableDefinition {

    // ── Switch defaults here ───────────────────────────
    // Change these constants to switch default strategy
    // without touching any other code
    private static final String DEFAULT_FILTER_TEMPLATE =
            "my_team_view";

    private static final String DEFAULT_COLUMN_PRESET =
            "default";
    // ──────────────────────────────────────────────────

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
    public String getSchema() { return "crm"; }

    @Override
    public String getTable() { return "interaction"; }

    @Override
    public String getDefaultFilterTemplate() {
        return DEFAULT_FILTER_TEMPLATE;
    }

    @Override
    public String getDefaultColumnPreset() {
        return DEFAULT_COLUMN_PRESET;
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

        // full preset uses wildcard — QueryBuilder
        // generates SELECT * instead of column list
        presets.put("full", ColumnPreset.builder()
                .name("full")
                .column("*")
                .build());

        return Collections.unmodifiableMap(presets);
    }

    @Override
    public Map<String, FilterTemplate>
            getFilterTemplates() {
        Map<String, FilterTemplate> templates =
                new HashMap<>();

        // ── STANDARD — open access ─────────────────────
        templates.put("open", FilterTemplate.builder()
                .name("open")
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
                    .fromEmployeeHeader(true)
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
                        .fromEmployeeHeader(true)
                        .build())
                    .templateParam(TemplateParam.builder()
                        .paramName("teamLevel")
                        .fromEmployeeHeader(false)
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
                    .templateParam(TemplateParam.builder()
                        .paramName("employeeId")
                        .fromEmployeeHeader(true)
                        .build())
                    .allowedParam("interaction_type")
                    .allowedParam("status")
                    .build());

        // ── VIEW — Redshift view (DEFAULT) ─────────────
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
                        .fromEmployeeHeader(true)
                        .build())
                    .allowedParam("interaction_type")
                    .allowedParam("status")
                    .build());

        return Collections.unmodifiableMap(templates);
    }
        }
