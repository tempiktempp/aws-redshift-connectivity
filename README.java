package com.edp.api.definition;

import lombok.Builder;
import lombok.Getter;

/**
 * Declares a single parameter needed by a
 * VIEW template.
 *
 * employeeId always comes from X-Employee-Id header.
 * All other params come from URL query params.
 *
 * Example:
 *   TemplateParam.builder()
 *       .paramName("employeeId")
 *       .fromEmployeeHeader(true)
 *       .build()
 *
 *   TemplateParam.builder()
 *       .paramName("teamLevel")
 *       .fromEmployeeHeader(false)
 *       .build()
 */
@Getter
@Builder
public class TemplateParam {

    /**
     * Named parameter as it appears in SQL.
     * e.g. "employeeId" for :employeeId in SQL
     */
    private final String paramName;

    /**
     * When true — value comes from X-Employee-Id header.
     * When false — value comes from URL query params.
     */
    @Builder.Default
    private final boolean fromEmployeeHeader = false;

    /**
     * Fixed hardcoded value.
     * When set — always uses this value regardless
     * of fromEmployeeHeader flag.
     */
    private final Object hardcodedValue;
}
