package com.edp.api.definition;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Map;
import java.util.Set;

/**
 * Defines a named filter template for a table.
 *
 * A filter template has two parts:
 *
 *   1. Fixed SQL fragment — always applied when this
 *      template is selected. Can be empty (open access),
 *      a simple WHERE clause, or a full CTE.
 *
 *   2. Allowed dynamic params — URL params the caller
 *      is allowed to pass on top of the fixed fragment.
 *      All combined with AND.
 *      Unknown params → 400 Bad Request.
 *
 * isCte = true means sqlFragment wraps the entire
 * base SELECT as a CTE rather than appending as WHERE.
 */
@Getter
@Builder
public class FilterTemplate {

    /** Unique name within the table definition */
    private final String name;

    /**
     * Fixed SQL fragment for this template.
     * Empty string = no fixed filters (open access).
     * CTE string = full entitlement wrapper.
     */
    @Builder.Default
    private final String sqlFragment = "";

    /**
     * When true, sqlFragment is a CTE that wraps
     * the entire base SELECT.
     * The base SELECT is injected via %s placeholder.
     * When false, sqlFragment appended as WHERE conditions.
     */
    @Builder.Default
    private final boolean isCte = false;

    /**
     * URL param names allowed dynamically on top
     * of this template. Unknown params → 400.
     */
    @Singular("allowedParam")
    private final Set<String> allowedParams;

    /**
     * Default values for named parameters in sqlFragment.
     * Applied when caller does not supply them.
     */
    @Singular("defaultParam")
    private final Map<String, Object> defaultParams;
}
