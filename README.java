package com.edp.api.definition;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines a named filter template for a table.
 *
 * templateType controls execution path:
 *
 *   STANDARD → QueryBuilder builds SQL dynamically
 *              sqlFragment = optional WHERE clause
 *
 *   CTE      → sqlFragment wraps base SELECT via %s
 *
 *   VIEW     → sqlFragment = full SQL executed as-is
 *              templateParams declare what to bind
 *              column presets NOT applied
 */
@Getter
@Builder
public class FilterTemplate {

    /** Unique name within the table definition */
    private final String name;

    /**
     * How this template executes.
     * Defaults to STANDARD if not set.
     */
    @Builder.Default
    private final TemplateType templateType =
            TemplateType.STANDARD;

    /**
     * SQL content — meaning depends on templateType:
     *   STANDARD → optional WHERE fragment
     *   CTE      → full CTE wrapping base SELECT via %s
     *   VIEW     → complete SQL executed as-is
     */
    @Builder.Default
    private final String sqlFragment = "";

    /**
     * Parameter declarations for VIEW templates.
     * Also used for STANDARD when params come
     * from header rather than URL params.
     */
    @Singular("templateParam")
    private final List<TemplateParam> templateParams;

    /**
     * URL param names caller can pass dynamically.
     * Applied on top of template with AND.
     * Unknown params → 400 Bad Request.
     */
    @Singular("allowedParam")
    private final Set<String> allowedParams;

    /**
     * Default values for named parameters.
     * Applied when caller does not supply them.
     */
    @Singular("defaultParam")
    private final Map<String, Object> defaultParams;
}
