package com.edp.api.exception;

/**
 * Thrown when caller requests a template name
 * that does not exist in the TableDefinition.
 * Maps to 400 Bad Request.
 */
public class InvalidTemplateException
        extends RuntimeException {

    public InvalidTemplateException(
            String templateName,
            String schema,
            String table) {
        super("Template '" + templateName + "' does not " +
              "exist for table: " + schema + "." + table);
    }
        }
