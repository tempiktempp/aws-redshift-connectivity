package com.edp.api.model.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Response wrapper for interaction search results.
 */
@Getter
@Builder
public class InteractionResponse {

    /** View type applied e.g. "My team" */
    private final String viewType;

    /** Total rows returned */
    private final int totalRows;

    /** Interaction rows */
    private final List<Map<String, Object>> rows;
}
