package com.edp.api.definition.tables;

import com.edp.api.definition.ColumnPreset;
import com.edp.api.definition.EntitlementFragments;
import com.edp.api.definition.FilterTemplate;
import com.edp.api.definition.ParamSource;
import com.edp.api.definition.TableDefinition;
import com.edp.api.definition.TemplateParam;
import com.edp.api.definition.TemplateType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Table definition for crm.interaction.
 *
 * Filter templates:
 *   default    → STANDARD, open access, no entitlement
 *   my_team    → STANDARD, entitlement via
 *                security.v_team_entitlement subquery
 *   my_team_by_level → STANDARD, entitlement filtered
 *                      by team level
 *
 * Column presets:
 *   default → core interaction columns
 *   summary → minimal columns for list views
 *   full    → all columns
 */
@Component
public class InteractionTableDefinition
        implements TableDefinition {

    @Override
    public String getSchema() { return "crm"; }

    @Override
    public String getTable() { return "interaction"; }

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
        return Map.of(

            "default", ColumnPreset.builder()
                    .name("default")
                    .column("id")
                    .column("employee_id")
                    .column("interaction_date")
                    .column("interaction_type")
                    .column("createddate")
                    .build(),

            "summary", ColumnPreset.builder()
                    .name("summary")
                    .column("id")
                    .column("interaction_date")
                    .column("interaction_type")
                    .build(),

            "full", ColumnPreset.builder()
                    .name("full")
                    .column("id")
                    .column("employee_id")
                    .column("interaction_date")
                    .column("interaction_type")
                    .column("createddate")
                    .build()
        );
    }

    @Override
    public Map<String, FilterTemplate>
            getFilterTemplates() {
        return Map.of(

            // ── STANDARD — open access ─────────────────
            "default", FilterTemplate.builder()
                    .name("default")
                    .templateType(TemplateType.STANDARD)
                    .sqlFragment("")
                    .allowedParam("interaction_type")
                    .allowedParam("status")
                    .build(),

            // ── STANDARD — team entitlement ────────────
            // Uses security.v_team_entitlement subquery.
            // Single query — no sequential calls needed.
            // Redshift optimises the full query plan.
            "my_team", FilterTemplate.builder()
                    .name("my_team")
                    .templateType(TemplateType.STANDARD)
                    .sqlFragment(
                        // Generates:
                        // employee_id IN (
                        //   SELECT team_member_id
                        //   FROM security.v_team_entitlement
                        //   WHERE source_employee_id
                        //         = :employeeId
                        // )
                        EntitlementFragments
                            .teamMemberFilter(
                                "employee_id"))
                    .templateParam(TemplateParam.builder()
                        .paramName("employeeId")
                        .source(ParamSource
                                .FROM_EMPLOYEE_HEADER)
                        .build())
                    .allowedParam("interaction_type")
                    .allowedParam("status")
                    .build(),

            // ── STANDARD — team entitlement by level ───
            // Same as my_team but filtered by team_level.
            // Caller passes ?teamLevel=1 to filter by
            // direct reports only.
            "my_team_by_level", FilterTemplate.builder()
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
                    .build()
        );
    }
            }
