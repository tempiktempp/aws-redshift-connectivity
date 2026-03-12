package com.edp.api.exception;

/**
 * Thrown when no TableDefinition is registered
 * for the requested schema.table combination.
 * Maps to 404 Not Found in GlobalExceptionHandler.
 */
public class TableNotFoundException extends RuntimeException {

    public TableNotFoundException(
            String schema,
            String table) {
        super("No table definition registered for: "
                + schema + "." + table);
    }
}
