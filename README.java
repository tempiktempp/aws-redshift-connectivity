package com.edp.api.controller;

import com.edp.api.facade.DataAccessFacade;
import com.edp.api.model.request.DataRequest;
import com.edp.api.model.response.DataResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Single generic controller for ALL table endpoints.
 *
 * URL structure:
 *   GET /api/v1/data/{schema}/{table}
 *   GET /api/v1/data/{schema}/{table}/views/{view}
 *   GET /api/v1/data/{schema}/{table}/{id}
 *
 * Adding a new table = zero changes here.
 * Just register a new TableDefinition bean.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/data")
public class GenericDataController {

    private final DataAccessFacade dataAccessFacade;

    // Internal params that must not be treated as filters
    private static final java.util.Set<String>
            RESERVED_PARAMS = java.util.Set.of(
                    "maxResults", "columns");

    /**
     * Fetch with default template.
     *
     * GET /api/v1/data/crm/interaction
     * GET /api/v1/data/crm/interaction?columns=summary
     * GET /api/v1/data/crm/interaction?maxResults=50
     * GET /api/v1/data/crm/interaction?status=OPEN
     */
    @GetMapping("/{schemaName}/{tableName}")
    public ResponseEntity<DataResponse> fetchWithDefaultView(
            @RequestHeader("X-Employee-Id")
            @NotBlank(message =
                    "X-Employee-Id header is required")
            String employeeId,

            @PathVariable
            @Pattern(
                regexp = "^[a-z_]{1,64}$",
                message = "schemaName must be lowercase " +
                          "letters and underscores only")
            String schemaName,

            @PathVariable
            @Pattern(
                regexp = "^[a-z_]{1,64}$",
                message = "tableName must be lowercase " +
                          "letters and underscores only")
            String tableName,

            @RequestParam(required = false)
            String columns,

            @RequestParam(required = false)
            @Min(value = 1,
                 message = "maxResults must be at least 1")
            @Max(value = 1000,
                 message = "maxResults cannot exceed 1000")
            Integer maxResults,

            @RequestParam(required = false)
            Map<String, String> allParams) {

        return ResponseEntity.ok(
                dataAccessFacade.fetchData(
                        buildRequest(
                                employeeId,
                                schemaName,
                                tableName,
                                null,
                                columns,
                                maxResults,
                                extractFilterParams(
                                        allParams))));
    }

    /**
     * Fetch with named view (filter template).
     *
     * GET /api/v1/data/crm/interaction/views/my_team
     * GET /api/v1/data/crm/interaction/views/my_team?columns=summary
     * GET /api/v1/data/crm/interaction/views/my_team?maxResults=50
     */
    @GetMapping("/{schemaName}/{tableName}/views/{viewName}")
    public ResponseEntity<DataResponse> fetchWithView(
            @RequestHeader("X-Employee-Id")
            @NotBlank(message =
                    "X-Employee-Id header is required")
            String employeeId,

            @PathVariable
            @Pattern(
                regexp = "^[a-z_]{1,64}$",
                message = "schemaName must be lowercase " +
                          "letters and underscores only")
            String schemaName,

            @PathVariable
            @Pattern(
                regexp = "^[a-z_]{1,64}$",
                message = "tableName must be lowercase " +
                          "letters and underscores only")
            String tableName,

            @PathVariable
            @Pattern(
                regexp = "^[a-z_]{1,64}$",
                message = "viewName must be lowercase " +
                          "letters and underscores only")
            String viewName,

            @RequestParam(required = false)
            String columns,

            @RequestParam(required = false)
            @Min(value = 1,
                 message = "maxResults must be at least 1")
            @Max(value = 1000,
                 message = "maxResults cannot exceed 1000")
            Integer maxResults,

            @RequestParam(required = false)
            Map<String, String> allParams) {

        return ResponseEntity.ok(
                dataAccessFacade.fetchData(
                        buildRequest(
                                employeeId,
                                schemaName,
                                tableName,
                                viewName,
                                columns,
                                maxResults,
                                extractFilterParams(
                                        allParams))));
    }

    /**
     * Fetch single row by ID.
     *
     * GET /api/v1/data/crm/interaction/INT001
     */
    @GetMapping("/{schemaName}/{tableName}/{id}")
    public ResponseEntity<DataResponse> fetchById(
            @RequestHeader("X-Employee-Id")
            @NotBlank(message =
                    "X-Employee-Id header is required")
            String employeeId,

            @PathVariable
            @Pattern(
                regexp = "^[a-z_]{1,64}$",
                message = "schemaName must be lowercase " +
                          "letters and underscores only")
            String schemaName,

            @PathVariable
            @Pattern(
                regexp = "^[a-z_]{1,64}$",
                message = "tableName must be lowercase " +
                          "letters and underscores only")
            String tableName,

            @PathVariable String id) {

        return ResponseEntity.ok(
                dataAccessFacade.fetchData(
                        DataRequest.builder()
                                .employeeId(employeeId)
                                .schemaName(schemaName)
                                .tableName(tableName)
                                .id(id)
                                .maxResults(1)
                                .build()));
    }

    // ── Private helpers ────────────────────────────────────────────

    private DataRequest buildRequest(
            String employeeId,
            String schemaName,
            String tableName,
            String viewName,
            String columns,
            Integer maxResults,
            Map<String, String> filterParams) {

        return DataRequest.builder()
                .employeeId(employeeId)
                .schemaName(schemaName)
                .tableName(tableName)
                .templateName(viewName)
                .columnPreset(columns)
                .maxResults(maxResults != null
                        ? maxResults : 100)
                .filterParams(filterParams)
                .build();
    }

    /**
     * Strips reserved params from allParams map
     * so only actual data filters are passed through.
     */
    private Map<String, String> extractFilterParams(
            Map<String, String> allParams) {
        if (allParams == null) return Map.of();
        Map<String, String> filters = new HashMap<>(allParams);
        RESERVED_PARAMS.forEach(filters::remove);
        return filters;
    }
            }
