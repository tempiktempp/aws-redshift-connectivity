package com.aws.utils.redshift.health;

import com.aws.utils.redshift.connection.RedshiftConnector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Spring Boot Actuator health indicator for Redshift connectivity.
 *
 * Automatically exposed at:
 *   GET /actuator/health/redshift
 *
 * Discovered automatically by Spring Boot Actuator via the
 * HealthIndicator interface — no extra configuration needed.
 *
 * Sample response when healthy:
 *   {
 *     "status": "UP",
 *     "details": {
 *       "strategy": "DATA_API"
 *     }
 *   }
 *
 * Sample response when unhealthy:
 *   {
 *     "status": "DOWN",
 *     "details": {
 *       "strategy": "DATA_API",
 *       "error": "Connection refused"
 *     }
 *   }
 *
 * Security note:
 *   Protect /actuator/health in production — either behind
 *   authentication or only accessible from internal networks.
 *   Stack traces are never exposed in the response.
 */
@Slf4j
@RequiredArgsConstructor
public class RedshiftHealthIndicator implements HealthIndicator {

    private final RedshiftConnector connector;

    @Override
    public Health health() {
        try {
            boolean healthy = connector.isHealthy();

            if (healthy) {
                return Health.up()
                        .withDetail("strategy",
                                connector.getStrategyName())
                        .build();
            }

            return Health.down()
                    .withDetail("strategy",
                            connector.getStrategyName())
                    .withDetail("reason",
                            "Health check query returned no result")
                    .build();

        } catch (Exception e) {
            log.warn("[RedshiftHealth] Health check failed: {}",
                    e.getMessage());

            // Return message only — never expose stack trace
            // in health endpoint responses
            return Health.down()
                    .withDetail("strategy",
                            connector.getStrategyName())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
