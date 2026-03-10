package com.aws.utils.redshift.exception;

/**
 * Thrown when a Redshift query exceeds the configured timeout.
 *
 * Extends RedshiftQueryException so callers can either:
 *   - Catch RedshiftTimeoutException specifically to handle timeouts differently
 *   - Catch RedshiftQueryException broadly to handle all failures the same way
 *
 * When this is thrown, the timed-out query will have already been
 * cancelled on the Redshift side to avoid wasting cluster resources.
 */
public class RedshiftTimeoutException extends RedshiftQueryException {

    public RedshiftTimeoutException(String message) {
        super(message);
    }
}
```

---

### ✅ What you should see when Step 4 is done

Your model and exception packages should look like this:
```
com/aws/utils/redshift/
├── model/
│   ├── QueryRequest.java
│   └── QueryResult.java
└── exception/
    ├── RedshiftQueryException.java
    └── RedshiftTimeoutException.java
