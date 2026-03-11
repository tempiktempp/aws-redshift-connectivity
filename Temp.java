package com.edp.api.exception;

import com.aws.utils.redshift.exception.RedshiftQueryException;
import com.aws.utils.redshift.exception.RedshiftTimeoutException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for edp-api-service.
 *
 * Catches all exceptions thrown from controllers and services
 * and maps them to consistent RFC 7807-style error responses.
 *
 * Error response shape:
 *   {
 *     "traceId":   "uuid",         <- correlate with logs
 *     "status":    400,
 *     "error":     "BAD_REQUEST",
 *     "message":   "human readable message",
 *     "timestamp": "2024-01-01T00:00:00Z"
 *   }
 *
 * Security rules:
 *   - Stack traces are NEVER exposed in responses
 *   - Internal error messages are sanitised before returning
 *   - traceId lets engineers look up the full trace in logs
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Redshift query timeout.
     * Returns 504 Gateway Timeout.
     */
    @ExceptionHandler(RedshiftTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleTimeout(
            RedshiftTimeoutException ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("[ExceptionHandler] Redshift timeout. " +
                "traceId={}, error={}", traceId, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(errorBody(
                        traceId,
                        504,
                        "GATEWAY_TIMEOUT",
                        "Query timed out. Please try again " +
                        "or reduce the scope of your request."));
    }

    /**
     * Handles Redshift query failures.
     * Returns 502 Bad Gateway.
     */
    @ExceptionHandler(RedshiftQueryException.class)
    public ResponseEntity<Map<String, Object>> handleQueryException(
            RedshiftQueryException ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("[ExceptionHandler] Redshift query failed. " +
                "traceId={}, error={}", traceId, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(errorBody(
                        traceId,
                        502,
                        "BAD_GATEWAY",
                        "Failed to fetch data from Redshift. " +
                        "Please try again later."));
    }

    /**
     * Handles invalid arguments — bad table name, column name etc.
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        String traceId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Invalid argument. " +
                "traceId={}, error={}", traceId, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorBody(
                        traceId,
                        400,
                        "BAD_REQUEST",
                        ex.getMessage()));
    }

    /**
     * Handles @Validated constraint violations on controller params.
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex) {
        String traceId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Constraint violation. " +
                "traceId={}, error={}", traceId, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorBody(
                        traceId,
                        400,
                        "BAD_REQUEST",
                        ex.getMessage()));
    }

    /**
     * Catch-all for any unexpected exception.
     * Returns 500 Internal Server Error.
     * Never exposes internal error details to the caller.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("[ExceptionHandler] Unexpected error. " +
                "traceId={}", traceId, ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(
                        traceId,
                        500,
                        "INTERNAL_SERVER_ERROR",
                        "An unexpected error occurred. " +
                        "Please contact support with traceId: "
                        + traceId));
    }

    // ── Private helper ─────────────────────────────────────────────

    private Map<String, Object> errorBody(String traceId,
                                           int status,
                                           String error,
                                           String message) {
        return Map.of(
                "traceId",   traceId,
                "status",    status,
                "error",     error,
                "message",   message,
                "timestamp", Instant.now().toString()
        );
    }
}
```

---

### ✅ Final project structure
```
edp-parent/
├── pom.xml
├── aws-redshift-utils/
│   ├── pom.xml
│   └── src/main/java/com/aws/utils/redshift/
│       ├── config/
│       │   ├── RedshiftProperties.java
│       │   └── RedshiftAutoConfiguration.java
│       ├── connection/
│       │   ├── RedshiftConnector.java
│       │   ├── DataApiRedshiftConnector.java
│       │   └── JdbcRedshiftConnector.java
│       ├── executor/
│       │   └── RedshiftQueryExecutor.java
│       ├── model/
│       │   ├── QueryRequest.java
│       │   └── QueryResult.java
│       ├── exception/
│       │   ├── RedshiftQueryException.java
│       │   └── RedshiftTimeoutException.java
│       ├── health/
│       │   └── RedshiftHealthIndicator.java
│       └── util/
│           ├── CredentialsProviderFactory.java
│           └── SecretsManagerUtil.java
└── edp-api-service/
    ├── pom.xml
    └── src/main/
        ├── java/com/edp/api/
        │   ├── controller/
        │   │   └── RedshiftDataController.java
        │   ├── service/
        │   │   └── RedshiftDataService.java
        │   └── exception/
        │       └── GlobalExceptionHandler.java
        └── resources/
            └── application.yml
```

---

### 9.5 — Final build check

Run this at the **root `edp-parent` level**:
```
mvn clean install -DskipTests
```

You should see:
```
[INFO] edp-parent .......................... SUCCESS
[INFO] aws-redshift-utils .................. SUCCESS
[INFO] edp-api-service ..................... SUCCESS
[INFO] BUILD SUCCESS
```

---

### 9.6 — Test the API locally

Set these environment variables in IntelliJ:
```
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your-key
AWS_SECRET_ACCESS_KEY=your-secret
REDSHIFT_DATABASE=your-db
REDSHIFT_CLUSTER_ID=your-cluster-id
REDSHIFT_DB_USER=your-db-user
```

Go to **Run** → **Edit Configurations** → your Spring Boot run config → **Environment Variables** and add them there.

Then run the main class and hit:
```
GET http://localhost:8080/api/v1/redshift/tables/orders
GET http://localhost:8080/api/v1/redshift/tables/orders?status=OPEN
GET http://localhost:8080/api/v1/redshift/tables/orders/123
GET http://localhost:8080/actuator/health/redshift
