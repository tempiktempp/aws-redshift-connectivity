package com.edp.api.controller;

import com.edp.api.model.request.InteractionFilterRequest;
import com.edp.api.model.response.InteractionResponse;
import com.edp.api.service.InteractionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for interaction search API.
 *
 * POST /api/v1/interactions/search
 *
 * Accepts JSON body with filter params.
 * employeeId comes from X-Employee-Id header.
 *
 * Example request:
 * {
 *   "viewType":  "My team",
 *   "typeDesc":  "Phone Call",
 *   "clientId":  "ACC001",
 *   "dateFrom":  "2024-01-01 00:00:00",
 *   "dateTo":    "2024-12-31 23:59:59"
 * }
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interactions")
public class InteractionController {

    private final InteractionService interactionService;

    /**
     * Search interactions with filters.
     *
     * POST /api/v1/interactions/search
     *
     * @param employeeId  from X-Employee-Id header
     * @param request     JSON body with filter params
     * @param maxResults  optional max rows, default 100
     */
    @PostMapping("/search")
    public ResponseEntity<InteractionResponse>
            searchInteractions(
                @RequestHeader("X-Employee-Id")
                @NotBlank(message =
                        "X-Employee-Id header is required")
                String employeeId,

                @RequestBody
                @Valid
                InteractionFilterRequest request,

                @RequestParam(defaultValue = "100")
                @Min(value = 1,
                     message = "maxResults must be " +
                               "at least 1")
                @Max(value = 1000,
                     message = "maxResults cannot " +
                               "exceed 1000")
                int maxResults) {

        log.info("[InteractionController] Search. " +
                "employee={}, viewType={}",
                employeeId, request.getViewType());

        InteractionResponse response =
                interactionService.fetchInteractions(
                        request, employeeId, maxResults);

        return ResponseEntity.ok(response);
    }
}
```

---

### Test endpoints
```
POST http://localhost:8080/api/v1/interactions/search
X-Employee-Id: EMP001
Content-Type: application/json

// My team — all filters
{
  "viewType": "My team",
  "typeDesc": "Phone Call",
  "clientId": "ACC001",
  "dateFrom": "2024-01-01 00:00:00",
  "dateTo":   "2024-12-31 23:59:59"
}

// My interaction — no optional filters
{
  "viewType": "My interaction"
}

// My team — date range only
{
  "viewType": "My team",
  "dateFrom": "2024-01-01 00:00:00",
  "dateTo":   "2024-12-31 23:59:59"
}

// Missing viewType → 400 Bad Request
{
  "typeDesc": "Phone Call"
}

// Invalid viewType → 400 Bad Request
{
  "viewType": "Invalid"
    }
