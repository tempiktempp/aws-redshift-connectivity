package com.edp.api.exception;

/**
 * Thrown when caller requests a column preset name
 * that does not exist in the TableDefinition.
 * Maps to 400 Bad Request.
 */
public class InvalidColumnPresetException
        extends RuntimeException {

    public InvalidColumnPresetException(
            String presetName,
            String schema,
            String table) {
        super("Column preset '" + presetName + "' does " +
              "not exist for table: " +
              schema + "." + table);
    }
        }
