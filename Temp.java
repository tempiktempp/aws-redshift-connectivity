package com.aws.utils.redshift.util;

import com.aws.utils.redshift.config.RedshiftProperties.CredentialsStrategy;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;

/**
 * Factory that creates the correct AwsCredentialsProvider
 * based on the configured CredentialsStrategy.
 *
 * Strategy breakdown:
 *
 *   IAM_ROLE (recommended)
 *     Uses DefaultCredentialsProvider which resolves credentials
 *     in this order automatically:
 *       1. Environment variables
 *       2. System properties
 *       3. Web Identity Token (EKS)
 *       4. EC2 / ECS instance role  ← what you want in production
 *     Zero credential management — just assign an IAM role
 *     to your EC2/ECS task and this works automatically.
 *
 *   ENVIRONMENT
 *     Reads AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
 *     directly from environment variables.
 *     Useful for local development and CI/CD pipelines.
 *
 *   SECRETS_MANAGER
 *     Still uses DefaultCredentialsProvider to authenticate
 *     with AWS (to call Secrets Manager itself).
 *     The actual Redshift credentials are then fetched
 *     separately by SecretsManagerUtil.
 *
 * This is a static utility — not a Spring bean.
 * Called internally by RedshiftAutoConfiguration only.
 */
@Slf4j
public final class CredentialsProviderFactory {

    // Utility class — no instantiation
    private CredentialsProviderFactory() {}

    /**
     * Creates an AwsCredentialsProvider for the given strategy.
     *
     * @param strategy the credentials strategy from config
     * @return the appropriate provider implementation
     */
    public static AwsCredentialsProvider create(
            CredentialsStrategy strategy) {
        return switch (strategy) {

            case IAM_ROLE -> {
                log.debug("[CredentialsFactory] Using " +
                        "DefaultCredentialsProvider (IAM Role chain)");
                // Resolves: env vars → system props →
                // web identity → EC2/ECS instance role
                yield DefaultCredentialsProvider.create();
            }

            case ENVIRONMENT -> {
                log.debug("[CredentialsFactory] Using " +
                        "EnvironmentVariableCredentialsProvider");
                yield EnvironmentVariableCredentialsProvider.create();
            }

            case SECRETS_MANAGER -> {
                log.debug("[CredentialsFactory] Using " +
                        "DefaultCredentialsProvider " +
                        "(for Secrets Manager access)");
                // IAM role is still needed to call Secrets Manager.
                // DefaultCredentialsProvider handles this via
                // the instance role automatically.
                yield DefaultCredentialsProvider.create();
            }
        };
    }
}
