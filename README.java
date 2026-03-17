package com.edp.api.definition;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Set;

/**
 * Defines a named column preset for a table.
 *
 * Each preset declares exactly which columns are
 * returned when selected by the caller via
 * ?columns=presetName
 *
 * Examples:
 *   default  → core columns only
 *   summary  → minimal columns for list views
 *   full     → all available columns
 */
@Getter
@Builder
public class ColumnPreset {

    /** Unique name within the table definition */
    private final String name;

    /** Columns returned when this preset is selected */
    @Singular("column")
    private final Set<String> columns;
}
