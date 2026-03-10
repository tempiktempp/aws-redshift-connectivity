package com.aws.utils.redshift.config;

import com.aws.utils.redshift.connection.DataApiRedshiftConnector;
import com.aws.utils.redshift.connection.JdbcRedshiftConnector;
import com.aws.utils.redshift.connection.RedshiftConnector;
import com.aws.utils.redshift.executor.RedshiftQueryExecutor;
import com.aws.utils.redshift.health.RedshiftHealthIndicator;
import com.aws.utils.redshift.util.CredentialsProviderFactory;
import com.aws.utils.redshift.util.SecretsManagerUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import javax.sql.DataSource;

/**
 * Spring Boot Auto-Configuration for aws-redshift-utils.
 *
 * This class wires up all beans automatically when this library
 * is on the classpath. Consuming projects (e.g. edp-api-service)
 * get a fully configured RedshiftQueryExecutor injected without
 * writing any configuration code themselves.
 *
 * How strategy selection works:
 *
 *   redshift.connection-strategy=DATA_API (default)
 *     -> registers DataApiRedshiftConnector
 *     -> registers RedshiftDataClient
 *
 *   redshift.connection-strategy=JDBC
 *     -> registers JdbcRedshiftConnector
 *     -> registers HikariCP DataSource
 *
 * All beans use @ConditionalOnMissingBean so any consuming project
 * can override any bean by simply declaring their own version.
 *
 * Registered as an auto-configuration via:
 *   META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(RedshiftProperties.class)
public class RedshiftAutoConfiguration {

    // ── Shared beans ───────────────────────────────────────────────

    /**
     * Resolves AWS credentials based on configured strategy.
     * Uses IAM Role by default — no credentials stored anywhere.
     */
    @Bean
    @ConditionalOnMissingBean
    public AwsCredentialsProvider awsCredentialsProvider(
            RedshiftProperties props) {
        log.info("[Redshift] Credentials strategy: {}",
                props.getCredentialsStrategy());
        return CredentialsProviderFactory.create(
                props.getCredentialsStrategy());
    }

    /**
     * Secrets Manager client.
     * Only created when credentialsStrategy = SECRETS_MANAGER.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "redshift",
            name = "credentials-strategy",
            havingValue = "SECRETS_MANAGER")
    public SecretsManagerClient secretsManagerClient(
            RedshiftProperties props,
            AwsCredentialsProvider credentialsProvider) {
        return SecretsManagerClient.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /**
     * Utility for fetching secrets from Secrets Manager.
     * Only created when credentialsStrategy = SECRETS_MANAGER.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "redshift",
            name = "credentials-strategy",
            havingValue = "SECRETS_MANAGER")
    public SecretsManagerUtil secretsManagerUtil(
            SecretsManagerClient secretsManagerClient) {
        return new SecretsManagerUtil(secretsManagerClient);
    }

    // ── DATA_API strategy ──────────────────────────────────────────

    /**
     * AWS Redshift Data API client.
     * Only created when connection-strategy = DATA_API (default).
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "redshift",
            name = "connection-strategy",
            havingValue = "DATA_API",
            matchIfMissing = true)
    public RedshiftDataClient redshiftDataClient(
            RedshiftProperties props,
            AwsCredentialsProvider credentialsProvider) {
        log.info("[Redshift] Initialising Data API client, region: {}",
                props.getRegion());
        return RedshiftDataClient.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /**
     * Connector implementation backed by the Data API.
     * Only created when connection-strategy = DATA_API (default).
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(RedshiftConnector.class)
    @ConditionalOnProperty(
            prefix = "redshift",
            name = "connection-strategy",
            havingValue = "DATA_API",
            matchIfMissing = true)
    public RedshiftConnector dataApiRedshiftConnector(
            RedshiftDataClient redshiftDataClient,
            RedshiftProperties props) {
        log.info("[Redshift] Connector strategy: DATA_API");
        return new DataApiRedshiftConnector(redshiftDataClient, props);
    }

    // ── JDBC strategy ──────────────────────────────────────────────

    /**
     * HikariCP DataSource for direct JDBC connections.
     * Only created when connection-strategy = JDBC.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "redshift",
            name = "connection-strategy",
            havingValue = "JDBC")
    public DataSource redshiftDataSource(
            RedshiftProperties props,
            @Autowired(required = false)
            SecretsManagerUtil secretsManagerUtil) {

        RedshiftProperties.JdbcProperties jdbc = props.getJdbc();
        RedshiftProperties.PoolProperties pool = jdbc.getPool();

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(
                "com.amazon.redshift.jdbc42.Driver");
        config.setJdbcUrl(buildJdbcUrl(jdbc, props));
        config.setPoolName("RedshiftHikariPool");
        config.setMaximumPoolSize(pool.getMaximumPoolSize());
        config.setMinimumIdle(pool.getMinimumIdle());
        config.setConnectionTimeout(pool.getConnectionTimeoutMs());
        config.setIdleTimeout(pool.getIdleTimeoutMs());
        config.setMaxLifetime(pool.getMaxLifetimeMs());
        config.setConnectionTestQuery(
                pool.getConnectionTestQuery());

        // Logs a warning if a connection is held longer than 60s
        config.setLeakDetectionThreshold(60_000L);

        resolveJdbcCredentials(config, props, secretsManagerUtil);

        log.info("[Redshift] Initialising JDBC pool: host={}, " +
                        "database={}, poolSize={}",
                jdbc.getHost(),
                props.getDatabase(),
                pool.getMaximumPoolSize());

        return new HikariDataSource(config);
    }

    /**
     * Connector implementation backed by direct JDBC.
     * Only created when connection-strategy = JDBC.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(RedshiftConnector.class)
    @ConditionalOnProperty(
            prefix = "redshift",
            name = "connection-strategy",
            havingValue = "JDBC")
    public RedshiftConnector jdbcRedshiftConnector(
            DataSource redshiftDataSource,
            RedshiftProperties props) {
        log.info("[Redshift] Connector strategy: JDBC");
        return new JdbcRedshiftConnector(redshiftDataSource, props);
    }

    // ── Strategy-agnostic beans ────────────────────────────────────

    /**
     * The primary public API of this library.
     * This is the only bean consuming projects need to inject.
     * The underlying strategy is fully transparent to callers.
     */
    @Bean
    @ConditionalOnMissingBean
    public RedshiftQueryExecutor redshiftQueryExecutor(
            RedshiftConnector connector,
            RedshiftProperties props) {
        return new RedshiftQueryExecutor(connector, props);
    }

    /**
     * Health indicator — exposes /actuator/health/redshift.
     * Auto-discovered by Spring Boot Actuator.
     */
    @Bean
    @ConditionalOnMissingBean
    public RedshiftHealthIndicator redshiftHealthIndicator(
            RedshiftConnector connector) {
        return new RedshiftHealthIndicator(connector);
    }

    // ── Private helpers ────────────────────────────────────────────

    private String buildJdbcUrl(
            RedshiftProperties.JdbcProperties jdbc,
            RedshiftProperties props) {
        String ssl = jdbc.isSslEnabled()
                ? "ssl=true&sslfactory=com.amazon.redshift.ssl.NonValidatingFactory"
                : "";
        return String.format(
                "jdbc:redshift://%s:%d/%s?%s",
                jdbc.getHost(),
                jdbc.getPort(),
                props.getDatabase(),
                ssl);
    }

    private void resolveJdbcCredentials(
            HikariConfig config,
            RedshiftProperties props,
            SecretsManagerUtil secretsManagerUtil) {

        switch (props.getCredentialsStrategy()) {
            case SECRETS_MANAGER -> {
                if (secretsManagerUtil == null) {
                    throw new IllegalStateException(
                            "[Redshift] credentialsStrategy=SECRETS_MANAGER " +
                            "but SecretsManagerUtil is not available.");
                }
                SecretsManagerUtil.RedshiftSecret secret =
                        secretsManagerUtil.fetchRedshiftSecret(
                                props.getSecretArn());
                config.setUsername(secret.username());
                config.setPassword(secret.password());
                log.info("[Redshift] JDBC credentials loaded " +
                        "from Secrets Manager.");
            }
            case ENVIRONMENT -> {
                log.info("[Redshift] JDBC credentials expected " +
                        "from environment variables.");
            }
            case IAM_ROLE -> {
                log.info("[Redshift] JDBC using IAM Role auth.");
            }
        }
    }
}
```

---

### 5.3 — Register the Auto-Configuration

Create a new file called `org.springframework.boot.autoconfigure.AutoConfiguration.imports` inside `src/main/resources/META-INF/spring/`:
```
com.aws.utils.redshift.config.RedshiftAutoConfiguration
```

> **Important** — this is a plain text file with no extension other than `.imports`. This is how Spring Boot 3.x discovers auto-configurations. Without this file, `edp-api-service` won't pick up any of the beans from this library automatically.

---

### ✅ What you should see when Step 5 is done
```
com/aws/utils/redshift/
├── config/
│   ├── RedshiftProperties.java
│   └── RedshiftAutoConfiguration.java
└── resources/
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

You will see **red compile errors** in `RedshiftAutoConfiguration` on these imports:
```
DataApiRedshiftConnector    ← created in Step 6
JdbcRedshiftConnector       ← created in Step 6
RedshiftConnector           ← created in Step 6
RedshiftQueryExecutor       ← created in Step 7
RedshiftHealthIndicator     ← created in Step 8
CredentialsProviderFactory  ← created in Step 8
SecretsManagerUtil          ← created in Step 8
