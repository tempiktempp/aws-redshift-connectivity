package com.edp.api.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * JSON request body for interaction search.
 *
 * viewType is mandatory — determines entitlement logic.
 * All other filters are optional.
 *
 * viewType values:
 *   "My team"        → filter by team entitlement
 *   "My interaction" → filter by employee only
 */
@Getter
@Setter
public class InteractionFilterRequest {

    /**
     * Determines entitlement scope.
     * Required — must be "My team" or "My interaction"
     */
    @NotBlank(message = "viewType is required")
    private String viewType;

    /**
     * Filter by interaction type.
     * Maps to: interactiontype column
     * e.g. "Phone Call", "Email"
     */
    private String typeDesc;

    /**
     * Filter by account/client ID.
     * Maps to: accountid column
     */
    private String clientId;

    /**
     * Start of date range filter.
     * Maps to: starttime >= dateFrom
     * Format: yyyy-MM-dd HH:mm:ss
     */
    private String dateFrom;

    /**
     * End of date range filter.
     * Maps to: endtime <= dateTo
     * Format: yyyy-MM-dd HH:mm:ss
     */
    private String dateTo;
}
