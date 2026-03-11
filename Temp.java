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
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import javax.sql.DataSource;

/**
 * Spring Boot Auto-Configuration for aws-redshift-utils.
 *
 * Wires up all beans automatically when this library is on
 * the classpath. Consuming projects get a fully configured
 * RedshiftQueryExecutor without writing any configuration code.
 *
 * Registered via:
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(RedshiftProperties.class)
public class RedshiftAutoConfiguration {

    // ── Shared beans ───────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    public AwsCredentialsProvider awsCredentialsProvider(
            RedshiftProperties props) {
        log.info("[Redshift] Credentials strategy: {}",
                props.getCredentialsStrategy());
        return CredentialsProviderFactory.create(
                props.getCredentialsStrategy());
    }

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
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

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

        log.info("[Redshift] Initialising Data API client, " +
                "region: {}", props.getRegion());

        return RedshiftDataClient.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(credentialsProvider)
                // UrlConnectionHttpClient uses Java's built-in
                // HttpURLConnection — respects JVM proxy system
                // properties set via -Dhttps.proxyHost etc.
                // This is what allows corporate proxy to work.
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

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
        return new DataApiRedshiftConnector(
                redshiftDataClient, props);
    }

    // ── JDBC strategy ──────────────────────────────────────────────

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
        config.setLeakDetectionThreshold(60_000L);

        resolveJdbcCredentials(config, props, secretsManagerUtil);

        log.info("[Redshift] Initialising JDBC pool: " +
                        "host={}, database={}, poolSize={}",
                jdbc.getHost(),
                props.getDatabase(),
                pool.getMaximumPoolSize());

        return new HikariDataSource(config);
    }

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
        return new JdbcRedshiftConnector(
                redshiftDataSource, props);
    }

    // ── Strategy-agnostic beans ────────────────────────────────────

    /**
     * The only bean consuming projects need to inject.
     * Strategy beneath is fully transparent to callers.
     */
    @Bean
    @ConditionalOnMissingBean
    public RedshiftQueryExecutor redshiftQueryExecutor(
            RedshiftConnector connector,
            RedshiftProperties props) {
        return new RedshiftQueryExecutor(connector, props);
    }

    /**
     * Auto-discovered by Spring Boot Actuator.
     * Exposes /actuator/health/redshift automatically.
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
                ? "ssl=true&sslfactory=com.amazon.redshift" +
                  ".ssl.NonValidatingFactory"
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
                            "[Redshift] SECRETS_MANAGER strategy " +
                            "configured but SecretsManagerUtil " +
                            "is not available.");
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
