package com.edp.api.definition.tables;

import com.edp.api.definition.ColumnPreset;
import com.edp.api.definition.FilterTemplate;
import com.edp.api.definition.TableDefinition;
import com.edp.api.definition.TemplateType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Table definition for crm.contact.
 *
 * Filter templates:
 *   default → STANDARD, open access
 *
 * Column presets:
 *   default → core contact columns
 *   summary → minimal columns
 *   full    → all columns
 */
@Component
public class ContactTableDefinition
        implements TableDefinition {

    @Override
    public String getSchema() {
        return "crm";
    }

    @Override
    public String getTable() {
        return "contact";
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
                .column("name")
                .column("email")
                .column("status")
                .column("created_at")
                .build());

        presets.put("summary", ColumnPreset.builder()
                .name("summary")
                .column("id")
                .column("name")
                .column("status")
                .build());

        presets.put("full", ColumnPreset.builder()
                .name("full")
                .column("id")
                .column("name")
                .column("email")
                .column("phone")
                .column("status")
                .column("created_at")
                .column("updated_at")
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
                .allowedParam("status")
                .allowedParam("name")
                .build());

        return Collections.unmodifiableMap(templates);
    }
        }
