package com.edp.api.controller;

import com.aws.utils.redshift.model.QueryResult;
import com.edp.api.service.RedshiftDataService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing Redshift data fetch APIs.
 *
 * All endpoints are versioned under /api/v1/redshift.
 * Versioning from day one makes non-breaking evolution easier.
 *
 * Input validation is applied at the controller layer:
 *   - @Pattern on tableName prevents path traversal
 *   - @Min / @Max on maxResults prevents abuse
 *   - Business-level whitelist validation happens in the service
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/redshift")
public class RedshiftDataController {

    private final RedshiftDataService redshiftDataService;

    /**
     * Fetch all rows from a table with an optional status filter.
     *
     * GET /api/v1/redshift/tables/{tableName}
     * GET /api/v1/redshift/tables/{tableName}?status=OPEN
     * GET /api/v1/redshift/tables/{tableName}?status=OPEN&maxResults=50
     *
     * @param tableName  name of the table — only lowercase letters
     *                   and underscores allowed
     * @param status     optional filter on status column
     * @param maxResults max rows to return, default 100, max 1000
     */
    @GetMapping("/tables/{tableName}")
    public ResponseEntity<List<Map<String, Object>>> fetchTableData(
            @PathVariable
            @Pattern(
                regexp = "^[a-z_]{1,64}$",
                message = "tableName must be lowercase letters " +
                          "and underscores only")
            String tableName,

            @RequestParam(required = false)
            String status,

            @RequestParam(defaultValue = "100")
            @Min(value = 1, message = "maxResults must be at least 1")
            @Max(value = 1000, message = "maxResults cannot exceed 1000")
            int maxResults) {

        log.info("[Controller] Fetch table data. " +
                        "table={}, status={}, maxResults={}",
                tableName, status, maxResults);

        List<Map<String, Object>> rows =
                redshiftDataService.fetchTableData(
                        tableName, status, maxResults);

        return ResponseEntity.ok(rows);
    }

    /**
     * Fetch a single row by ID from a table.
     *
     * GET /api/v1/redshift/tables/{tableName}/{id}
     *
     * @param tableName name of the table
     * @param id        the row ID to look up
     */
    @GetMapping("/tables/{tableName}/{id}")
    public ResponseEntity<Map<String, Object>> fetchById(
            @PathVariable
            @Pattern(
                regexp = "^[a-z_]{1,64}$",
                message = "tableName must be lowercase letters " +
                          "and underscores only")
            String tableName,

            @PathVariable String id) {

        log.info("[Controller] Fetch by ID. table={}, id={}",
                tableName, id);

        Map<String, Object> row =
                redshiftDataService.fetchById(tableName, id);

        if (row.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(row);
    }

    /**
     * Returns the active connector strategy.
     * Useful for verifying which strategy is running in each environment.
     *
     * GET /api/v1/redshift/strategy
     */
    @GetMapping("/strategy")
    public ResponseEntity<Map<String, String>> getStrategy(
            RedshiftDataService service) {
        return ResponseEntity.ok(
                Map.of("strategy",
                        service.executeCustomQuery(
                                "SELECT 1", null, 1)
                               .getStrategy()));
    }
    }
