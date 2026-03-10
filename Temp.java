package com.aws.utils.redshift.connection;

import com.aws.utils.redshift.model.QueryRequest;
import com.aws.utils.redshift.model.QueryResult;

/**
 * Strategy interface for Redshift connectivity.
 *
 * Defines the contract for executing queries against AWS Redshift.
 * Two implementations are provided:
 *
 *   DataApiRedshiftConnector  — AWS Redshift Data API (recommended)
 *   JdbcRedshiftConnector     — Direct JDBC via HikariCP
 *
 * Consuming code depends only on this interface via
 * RedshiftQueryExecutor — the concrete implementation is
 * resolved automatically by Spring based on the configured
 * redshift.connection-strategy property.
 *
 * Switching strategies requires zero code changes —
 * just one line in application.yml.
 *
 * Design pattern: Strategy
 */
public interface RedshiftConnector {

    /**
     * Executes a SQL query against Redshift and returns results.
     *
     * Implementations are responsible for:
     *   - Binding named parameters safely
     *   - Enforcing the configured timeout
     *   - Paginating through large result sets
     *   - Cleaning up all resources after execution
     *
     * @param request query containing SQL, parameters and options
     * @return QueryResult containing rows and metadata
     * @throws com.aws.utils.redshift.exception.RedshiftQueryException
     *         on query failure
     * @throws com.aws.utils.redshift.exception.RedshiftTimeoutException
     *         on timeout — query will have been cancelled on Redshift side
     */
    QueryResult executeQuery(QueryRequest request);

    /**
     * Lightweight connectivity check used by the health indicator.
     * Implementations run a minimal query e.g. SELECT 1.
     *
     * @return true if connection to Redshift is healthy
     */
    boolean isHealthy();

    /**
     * Returns the strategy name for logging and monitoring.
     *
     * @return "DATA_API" or "JDBC"
     */
    String getStrategyName();
}
