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
 * URL structure:
 *   /api/v1/redshift/schemas/{schema}/tables/{table}
 *
 * Pattern validation on path variables prevents:
 *   - SQL injection via path e.g. crm; DROP TABLE
 *   - Path traversal attacks
 *   - Unexpected characters in identifiers
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/redshift")
public class RedshiftDataController {

    private final RedshiftDataService redshiftDataService;

    /**
     * Fetch all rows from a schema.table with optional filters.
     *
     * GET /api/v1/redshift/schemas/crm/tables/contact
     * GET /api/v1/redshift/schemas/crm/tables/contact?status=OPEN
     * GET /api/v1/redshift/schemas/crm/tables/contact?maxResults=50
     */
    @GetMapping("/schemas/{schemaName}/tables/{tableName}")
    public ResponseEntity<List<Map<String, Object>>> fetchTableData(
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
            String status,

            @RequestParam(defaultValue = "100")
            @Min(value = 1, message = "maxResults must be at least 1")
            @Max(value = 1000, message = "maxResults cannot exceed 1000")
            int maxResults) {

        log.info("[Controller] Fetch table data. " +
                        "schema={}, table={}, " +
                        "status={}, maxResults={}",
                schemaName, tableName, status, maxResults);

        List<Map<String, Object>> rows =
                redshiftDataService.fetchTableData(
                        schemaName, tableName,
                        status, maxResults);

        return ResponseEntity.ok(rows);
    }

    /**
     * Fetch a single row by ID.
     *
     * GET /api/v1/redshift/schemas/crm/tables/contact/123
     */
    @GetMapping("/schemas/{schemaName}/tables/{tableName}/{id}")
    public ResponseEntity<Map<String, Object>> fetchById(
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

        log.info("[Controller] Fetch by ID. " +
                        "schema={}, table={}, id={}",
                schemaName, tableName, id);

        Map<String, Object> row =
                redshiftDataService.fetchById(
                        schemaName, tableName, id);

        if (row.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(row);
    }
}
