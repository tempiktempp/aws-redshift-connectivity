package com.aws.utils.redshift.exception;

/**
 * Thrown when a Redshift query fails at the execution level.
 *
 * Wraps underlying AWS SDK or JDBC exceptions with a single
 * consistent exception type so consuming code only needs to
 * catch one exception regardless of which strategy is active.
 *
 * Example:
 *   try {
 *       queryExecutor.executeQuery(request);
 *   } catch (RedshiftQueryException e) {
 *       // handles both DATA_API and JDBC failures
 *   }
 */
public class RedshiftQueryException extends RuntimeException {

    public RedshiftQueryException(String message) {
        super(message);
    }

    public RedshiftQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
