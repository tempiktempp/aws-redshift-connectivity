package com.aws.utils.redshift.util;

import com.aws.utils.redshift.exception.RedshiftQueryException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import java.util.Map;

/**
 * Utility for fetching and parsing secrets from AWS Secrets Manager.
 *
 * Redshift secrets are expected to be JSON with at minimum:
 *   {
 *     "username": "redshift_api_user",
 *     "password": "s3cur3-p@ssw0rd"
 *   }
 *
 * This matches the format used by the AWS-managed Redshift
 * rotation Lambda — so secrets created via the AWS Console
 * "Credentials for Amazon Redshift" option work out of the box.
 *
 * Security notes:
 *   - Password is held in memory only during DataSource init
 *   - Password is NEVER logged anywhere in this class
 *   - toString() on RedshiftSecret explicitly redacts password
 *     to prevent accidental exposure in logs or exceptions
 */
@Slf4j
@RequiredArgsConstructor
public class SecretsManagerUtil {

    private final SecretsManagerClient secretsManagerClient;

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    /**
     * Fetches and parses a Redshift secret from Secrets Manager.
     *
     * @param secretArn the full ARN of the secret
     * @return RedshiftSecret record with username and password
     * @throws RedshiftQueryException if secret cannot be fetched
     *         or does not contain required keys
     */
    public RedshiftSecret fetchRedshiftSecret(String secretArn) {
        if (secretArn == null || secretArn.isBlank()) {
            throw new IllegalArgumentException(
                    "secretArn must not be null or blank.");
        }

        // Log partial ARN only — never log the full ARN
        // as it can contain account IDs
        log.info("[SecretsManager] Fetching Redshift credentials. " +
                        "secret=...{}",
                secretArn.substring(
                        Math.max(0, secretArn.length() - 8)));

        try {
            GetSecretValueRequest request =
                    GetSecretValueRequest.builder()
                            .secretId(secretArn)
                            .build();

            GetSecretValueResponse response =
                    secretsManagerClient.getSecretValue(request);

            String secretJson = response.secretString();

            if (secretJson == null || secretJson.isBlank()) {
                throw new RedshiftQueryException(
                        "Secret value is empty for ARN: "
                        + secretArn);
            }

            @SuppressWarnings("unchecked")
            Map<String, String> secretMap =
                    OBJECT_MAPPER.readValue(secretJson, Map.class);

            String username = secretMap.get("username");
            String password = secretMap.get("password");

            if (username == null || password == null) {
                throw new RedshiftQueryException(
                        "Secret must contain 'username' and " +
                        "'password' keys.");
            }

            // Log username only — never log password
            log.info("[SecretsManager] Credentials loaded " +
                    "for user: {}", username);

            return new RedshiftSecret(username, password);

        } catch (SecretsManagerException e) {
            throw new RedshiftQueryException(
                    "Failed to retrieve secret from " +
                    "Secrets Manager: " + e.getMessage(), e);
        } catch (RedshiftQueryException e) {
            throw e;
        } catch (Exception e) {
            throw new RedshiftQueryException(
                    "Failed to parse Redshift secret JSON: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Immutable record holding Redshift credentials.
     *
     * toString() deliberately redacts the password to prevent
     * accidental exposure if this object is ever logged.
     *
     * @param username Redshift database username
     * @param password Redshift database password
     */
    public record RedshiftSecret(String username, String password) {

        @Override
        public String toString() {
            return "RedshiftSecret{" +
                    "username='" + username + "'" +
                    ", password='[REDACTED]'" +
                    "}";
        }
    }
}
```

---

### 8.4 — Verify `AutoConfiguration.imports` file

Open `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and confirm it contains exactly this one line:
```
com.aws.utils.redshift.config.RedshiftAutoConfiguration
```

No extra spaces, no blank lines before or after.

---

### ✅ What you should see when Step 8 is done

Complete `aws-redshift-utils` structure:
```
com/aws/utils/redshift/
├── config/
│   ├── RedshiftProperties.java
│   └── RedshiftAutoConfiguration.java
├── connection/
│   ├── RedshiftConnector.java
│   ├── DataApiRedshiftConnector.java
│   └── JdbcRedshiftConnector.java
├── executor/
│   └── RedshiftQueryExecutor.java
├── model/
│   ├── QueryRequest.java
│   └── QueryResult.java
├── exception/
│   ├── RedshiftQueryException.java
│   └── RedshiftTimeoutException.java
├── health/
│   └── RedshiftHealthIndicator.java
└── util/
    ├── CredentialsProviderFactory.java
    └── SecretsManagerUtil.java
```

**Zero compile errors** across all files in `aws-redshift-utils`.

---

### 8.5 — Build the module

Run this in the terminal at the `aws-redshift-utils` level to confirm everything compiles and installs to your local `.m2`:
```
mvn clean install -DskipTests
```

You should see:
```
BUILD SUCCESS
Installing aws-redshift-utils-1.0.0.jar to ~/.m2/...
