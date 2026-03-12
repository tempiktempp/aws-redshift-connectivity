package com.edp.api.exception;

/**
 * Thrown when caller passes a filter param
 * not declared in the template's allowedParams.
 * Maps to 400 Bad Request.
 */
public class InvalidFilterParamException
        extends RuntimeException {

    public InvalidFilterParamException(
            String paramName,
            String templateName) {
        super("Filter param '" + paramName + "' is not " +
              "allowed for template: '" + templateName +
              "'");
    }
        }
