package com.edp.api.model.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Generic response wrapper for all data fetch endpoints.
 */
@Getter
@Builder
public class DataResponse {

    /** Schema queried */
    private final String schema;

    /** Table queried */
    private final String table;

    /** Filter template applied */
    private final String appliedTemplate;

    /** Column preset applied */
    private final String appliedColumnPreset;

    /** Number of rows returned */
    private final int totalRows;

    /** Data rows — column name to value */
    private final List<Map<String, Object>> rows;
}
