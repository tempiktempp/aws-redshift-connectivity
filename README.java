package com.edp.api.definition;

/**
 * Declares where a template parameter value
 * comes from at runtime.
 *
 * FROM_EMPLOYEE_HEADER → value comes from
 *   X-Employee-Id request header
 *
 * FROM_REQUEST_PARAM → value comes from
 *   URL query params passed by caller
 *
 * HARDCODED → value is fixed in the template
 *   definition — never from caller
 */
public enum ParamSource {
    FROM_EMPLOYEE_HEADER,
    FROM_REQUEST_PARAM,
    HARDCODED
}
