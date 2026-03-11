spring:
  application:
    name: edp-api-service
  # Disable security auto-config for now — we'll add it later
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

server:
  port: 8080

# ── Redshift Configuration ─────────────────────────────────────────
# All sensitive values injected from environment variables.
# Never hardcode real values here — this file is committed to git.
redshift:
  connection-strategy: DATA_API       # DATA_API or JDBC
  credentials-strategy: ENVIRONMENT   # use ENVIRONMENT for local dev
  region: ${AWS_REGION:us-east-1}
  database: ${REDSHIFT_DATABASE:dev}
  cluster-identifier: ${REDSHIFT_CLUSTER_ID:your-cluster-id}
  db-user: ${REDSHIFT_DB_USER:your-db-user}
  query:
    timeout-seconds: 300
    max-results-per-page: 1000
    poll-interval-ms: 500

# ── Actuator ───────────────────────────────────────────────────────
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: always            # change to when-authorized in prod

# ── Logging ────────────────────────────────────────────────────────
logging:
  level:
    com.edp: DEBUG
    com.aws.utils: DEBUG
