package com.edp.api.definition;

/**
 * Defines how a FilterTemplate executes its query.
 *
 * STANDARD → QueryBuilder builds SQL dynamically
 *            sqlFragment = optional WHERE clause
 *
 * CTE      → sqlFragment is a full CTE that wraps
 *            the base SELECT via %s placeholder
 *
 * VIEW     → sqlFragment is complete SQL executed as-is
 *            Column presets NOT applied
 *
 * PROC     → sqlFragment is a CALL statement
 *            followed by SELECT from temp table
 *            Column presets NOT applied
 */
public enum TemplateType {
    STANDARD,
    CTE,
    VIEW,
    PROC
}
