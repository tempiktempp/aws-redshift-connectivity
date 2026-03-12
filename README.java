package com.edp.api.definition;

import java.util.Map;

/**
 * Defines the full configuration for a single Redshift table.
 *
 * Every table accessible via the API must have exactly one
 * TableDefinition registered as a Spring bean.
 *
 * Unknown tables are rejected at facade level before
 * any SQL is built — critical security control.
 *
 * Adding a new table:
 *   1. Create a class implementing TableDefinition
 *   2. Annotate with @Component
 *   3. Define column presets, filter templates, strategies
 *   4. Done — zero other changes needed
 */
public interface TableDefinition {

    /** Redshift schema name e.g. "crm" */
    String getSchema();

    /** Redshift table name e.g. "interaction" */
    String getTable();

    /**
     * All named column presets for this table.
     * Key = preset name e.g. "default", "summary", "full"
     * Value = ColumnPreset defining which columns to return
     */
    Map<String, ColumnPreset> getColumnPresets();

    /**
     * All named filter templates for this table.
     * Key = template name e.g. "default", "my_team"
     * Value = FilterTemplate defining SQL + allowed params
     */
    Map<String, FilterTemplate> getFilterTemplates();

    /**
     * Name of the default column preset.
     * Applied when caller does not specify ?columns=
     */
    String getDefaultColumnPreset();

    /**
     * Name of the default filter template.
     * Applied when caller does not specify a view name.
     */
    String getDefaultFilterTemplate();

    /**
     * Unique registry key — do not override.
     * Used by TableRegistry for lookup.
     */
    default String getRegistryKey() {
        return getSchema() + "." + getTable();
    }
}
