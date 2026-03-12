package com.edp.api.definition.tables;

import com.edp.api.definition.ColumnPreset;
import com.edp.api.definition.FilterTemplate;
import com.edp.api.definition.TableDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Table definition for crm.interaction.
 *
 * Filter templates:
 *   default  → open access, no fixed filters
 *   my_team  → entitlement CTE, team-filtered
 *
 * Column presets:
 *   default  → core interaction columns
 *   summary  → minimal columns for list views
 *   full     → all columns including child table data
 */
@Component
public class InteractionTableDefinition
        implements TableDefinition {

    /**
     * Entitlement CTE template.
     * Wraps base SELECT with team-based filtering.
     * %s is replaced with the base SELECT at runtime.
     */
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
        return Map.of(

            "default", ColumnPreset.builder()
                    .name("default")
                    .column("id")
                    .column("employee_id")
                    .column("interaction_date")
                    .column("interaction_type")
                    .column("created_at")
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
                    .column("created_at")
                    .build()
        );
    }

    @Override
    public Map<String, FilterTemplate> getFilterTemplates() {
        return Map.of(

            // Open access — no fixed filters
            "default", FilterTemplate.builder()
                    .name("default")
                    .sqlFragment("")
                    .isCte(false)
                    .allowedParam("interaction_type")
                    .allowedParam("status")
                    .build(),

            // Entitlement CTE — team filtered with joins
            "my_team", FilterTemplate.builder()
                    .name("my_team")
                    .sqlFragment(ENTITLEMENT_CTE)
                    .isCte(true)
                    .allowedParam("interaction_type")
                    .allowedParam("status")
                    .build()
        );
    }
                }
