package com.aws.utils.redshift.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration properties for AWS Redshift connectivity.
 *
 * All properties are prefixed with "redshift" in application.yml.
 * Validated at startup — the app will fail fast with a clear error
 * message if any required property is missing or invalid.
 *
 * Two connection strategies are supported:
 *
 *   DATA_API (default, recommended)
 *     - No direct cluster exposure needed
 *     - IAM-native auth, no passwords to manage
 *     - Asynchronous — adds ~200-500ms polling overhead
 *
 *   JDBC
 *     - Direct TCP connection to cluster on port 5439
 *     - Synchronous — lower latency
 *     - Requires VPC access to the cluster
 *
 * Three credential strategies are supported:
 *
 *   IAM_ROLE (default, recommended for AWS deployments)
 *     - Uses EC2/ECS instance role automatically
 *     - Zero credential management
 *
 *   SECRETS_MANAGER
 *     - Fetches username/password from AWS Secrets Manager at startup
 *     - Required for JDBC strategy in most setups
 *
 *   ENVIRONMENT
 *     - Reads AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY env vars
 *     - Useful for local development and CI/CD pipelines
 *
 *
 * Example application.yml (DATA_API + IAM_ROLE):
 *
 *   redshift:
 *     connection-strategy: DATA_API
 *     credentials-strategy: IAM_ROLE
 *     region: us-east-1
 *     database: sales_db
 *     cluster-identifier: my-cluster
 *     db-user: api_user
 *     query:
 *       timeout-seconds: 300
 *       max-results-per-page: 1000
 *       poll-interval-ms: 500
 *
 *
 * Example application.yml (JDBC + SECRETS_MANAGER):
 *
 *   redshift:
 *     connection-strategy: JDBC
 *     credentials-strategy: SECRETS_MANAGER
 *     region: us-east-1
 *     database: sales_db
 *     secret-arn: arn:aws:secretsmanager:us-east-1:123456789:secret:prod/redshift
 *     jdbc:
 *       host: my-cluster.abc123.us-east-1.redshift.amazonaws.com
 *       port: 5439
 *       ssl-enabled: true
 *       pool:
 *         maximum-pool-size: 10
 *         minimum-idle: 2
 *         connection-timeout-ms: 30000
 *         idle-timeout-ms: 600000
 *         max-lifetime-ms: 1800000
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "redshift")
public class RedshiftProperties {

    // ── Connection strategy ────────────────────────────────────────

    /**
     * How the application connects to Redshift.
     * DATA_API is strongly recommended for all cloud deployments.
     */
    @NotNull
    private ConnectionStrategy connectionStrategy = ConnectionStrategy.DATA_API;

    /**
     * How AWS credentials are resolved.
     * IAM_ROLE is strongly recommended — never store credentials
     * in config files or environment variables in production.
     */
    @NotNull
    private CredentialsStrategy credentialsStrategy = CredentialsStrategy.IAM_ROLE;

    // ── Common ─────────────────────────────────────────────────────

    /** AWS region where the Redshift cluster is deployed. e.g. us-east-1 */
    @NotBlank
    private String region;

    /** Redshift database name to connect to. */
    @NotBlank
    private String database;

    /**
     * Redshift provisioned cluster identifier.
     * Required when connectionStrategy = DATA_API with a provisioned cluster.
     * Leave blank if using Redshift Serverless — use workgroupName instead.
     */
    private String clusterIdentifier;

    /**
     * Redshift Serverless workgroup name.
     * Use this instead of clusterIdentifier for Serverless deployments.
     */
    private String workgroupName;

    /**
     * AWS Secrets Manager secret ARN containing Redshift credentials.
     * Required when credentialsStrategy = SECRETS_MANAGER.
     * The secret must be JSON with keys: "username" and "password".
     */
    private String secretArn;

    /**
     * Redshift database user.
     * Used with DATA_API when not using Secrets Manager auth.
     * Never use a superuser account here in production.
     */
    private String dbUser;

    // ── Nested config ──────────────────────────────────────────────

    @Valid
    private JdbcProperties jdbc = new JdbcProperties();

    @Valid
    private QueryProperties query = new QueryProperties();

    // ── Enums ──────────────────────────────────────────────────────

    public enum ConnectionStrategy {
        /** AWS Redshift Data API — no direct cluster exposure, IAM-native */
        DATA_API,
        /** Direct JDBC via HikariCP — lower latency, requires VPC access */
        JDBC
    }

    public enum CredentialsStrategy {
        /** EC2/ECS/Lambda IAM Role — recommended, zero credential management */
        IAM_ROLE,
        /** Fetch credentials from AWS Secrets Manager at startup */
        SECRETS_MANAGER,
        /** Read AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY from environment */
        ENVIRONMENT
    }

    // ── Nested config classes ──────────────────────────────────────

    @Getter
    @Setter
    public static class JdbcProperties {

        /**
         * Redshift cluster hostname.
         * Required when connectionStrategy = JDBC.
         */
        private String host;

        /** Redshift port. Default 5439 — only change if cluster is non-standard. */
        @Min(1) @Max(65535)
        private int port = 5439;

        /**
         * Always keep true in production.
         * Setting to false exposes credentials and data in transit.
         */
        private boolean sslEnabled = true;

        @Valid
        private PoolProperties pool = new PoolProperties();
    }

    @Getter
    @Setter
    public static class PoolProperties {

        /** Maximum connections in the pool. */
        @Min(1) @Max(100)
        private int maximumPoolSize = 10;

        /** Minimum idle connections maintained in the pool. */
        @Min(0)
        private int minimumIdle = 2;

        /** Max milliseconds to wait for a connection before throwing. */
        @Min(1000)
        private long connectionTimeoutMs = 30_000L;

        /** Max milliseconds a connection can sit idle before removal. */
        @Min(10000)
        private long idleTimeoutMs = 600_000L;

        /**
         * Max lifetime of a connection in the pool.
         * Should be several seconds less than the database timeout.
         */
        @Min(30000)
        private long maxLifetimeMs = 1_800_000L;

        /** Query used to validate connections on borrow. */
        private String connectionTestQuery = "SELECT 1";
    }

    @Getter
    @Setter
    public static class QueryProperties {

        /** Maximum seconds to wait for a query to complete. */
        @Min(1) @Max(3600)
        private int timeoutSeconds = 300;

        /**
         * Hard cap on rows returned per request.
         * Client requests cannot exceed this value.
         */
        @Min(1) @Max(10000)
        private int maxResultsPerPage = 1000;

        /** Milliseconds between Data API query status polls. */
        @Min(100) @Max(5000)
        private long pollIntervalMs = 500L;
    }
}
