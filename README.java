package com.edp.api.model.request;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Encapsulates all data needed to execute a data request.
 * Passed from controller through facade to query builder.
 */
@Getter
@Builder
public class DataRequest {

    /** Requesting employee — drives entitlement logic */
    private final String employeeId;

    /** Schema name — validated against TableDefinition */
    private final String schemaName;

    /** Table name — validated against TableDefinition */
    private final String tableName;

    /** Optional row ID for single row lookups */
    private final String id;

    /**
     * Filter template name selected by caller.
     * Null means use the table's default template.
     */
    private final String templateName;

    /**
     * Column preset name selected by caller.
     * Null means use the table's default column preset.
     */
    private final String columnPreset;

    /** Max rows to return */
    @Builder.Default
    private final int maxResults = 100;

    /**
     * Dynamic filter params passed by caller.
     * Must be declared in template's allowedParams.
     * Values bound as named parameters — never in SQL.
     */
    private final Map<String, String> filterParams;
}
