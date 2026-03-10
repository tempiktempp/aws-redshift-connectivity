package com.aws.utils.redshift.connection;

import com.aws.utils.redshift.config.RedshiftProperties;
import com.aws.utils.redshift.exception.RedshiftQueryException;
import com.aws.utils.redshift.model.QueryRequest;
import com.aws.utils.redshift.model.QueryResult;
import com.aws.utils.redshift.model.QueryResult.ColumnMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redshift connector backed by direct JDBC via HikariCP.
 *
 * Use this when:
 *   - Sub-100ms latency is required (no async polling overhead)
 *   - Your app runs in the same VPC as the Redshift cluster
 *   - Synchronous request-response patterns are needed
 *
 * Security properties:
 *   - All queries use NamedParameterJdbcTemplate with named
 *     parameters — prevents SQL injection at the value level
 *   - SSL enforced by default via the JDBC URL
 *   - Credentials injected at startup from Secrets Manager
 *     or IAM — never stored in this class
 *
 * Trade-off:
 *   Requires port 5439 reachable from the app network.
 *   Use DataApiRedshiftConnector if direct cluster access
 *   is not available or not desired.
 */
@Slf4j
public class JdbcRedshiftConnector implements RedshiftConnector {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RedshiftProperties props;

    private static final String STRATEGY_NAME = "JDBC";

    public JdbcRedshiftConnector(DataSource dataSource,
                                  RedshiftProperties props) {
        this.jdbcTemplate =
                new NamedParameterJdbcTemplate(dataSource);
        this.props = props;
    }

    // ── RedshiftConnector implementation ──────────────────────────

    @Override
    public QueryResult executeQuery(QueryRequest request) {
        log.debug("[Redshift-JDBC] Executing query, " +
                "database: {}", props.getDatabase());

        MapSqlParameterSource paramSource =
                buildParamSource(request);

        List<Map<String, Object>> rows = new ArrayList<>();
        List<ColumnMetadata> columnMetadata = new ArrayList<>();
        int[] rowCount = {0};
        int maxResults = effectiveMaxResults(request);

        try {
            jdbcTemplate.query(
                    request.getSql(),
                    paramSource,
                    rs -> {
                        // Extract column metadata on first row only
                        if (columnMetadata.isEmpty()) {
                            ResultSetMetaData meta =
                                    rs.getMetaData();
                            int colCount = meta.getColumnCount();
                            for (int i = 1; i <= colCount; i++) {
                                columnMetadata.add(
                                        new ColumnMetadata(
                                                meta.getColumnLabel(i),
                                                meta.getColumnTypeName(i)
                                        ));
                            }
                        }

                        // Hard cap on results
                        if (rowCount[0] >= maxResults) {
                            return;
                        }

                        Map<String, Object> row =
                                new LinkedHashMap<>();
                        for (ColumnMetadata col : columnMetadata) {
                            row.put(col.name(),
                                    rs.getObject(col.name()));
                        }
                        rows.add(row);
                        rowCount[0]++;
                    });

            log.debug("[Redshift-JDBC] Query complete, " +
                    "rows: {}", rows.size());

            return QueryResult.builder()
                    .rows(Collections.unmodifiableList(rows))
                    .columnMetadata(Collections.unmodifiableList(
                            columnMetadata))
                    .totalRows(rows.size())
                    .queryId(null) // no async ID for JDBC
                    .strategy(STRATEGY_NAME)
                    .build();

        } catch (org.springframework.dao.DataAccessException e) {
            throw new RedshiftQueryException(
                    "JDBC query execution failed: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            jdbcTemplate.queryForObject(
                    "SELECT 1",
                    new MapSqlParameterSource(),
                    Integer.class);
            return true;
        } catch (Exception e) {
            log.warn("[Redshift-JDBC] Health check failed: {}",
                    e.getMessage());
            return false;
        }
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    // ── Private helpers ────────────────────────────────────────────

    /**
     * Builds a named parameter source from the request parameters.
     * All values pass through Spring's binding layer —
     * prevents injection at the value level.
     */
    private MapSqlParameterSource buildParamSource(
            QueryRequest request) {
        MapSqlParameterSource source =
                new MapSqlParameterSource();
        if (request.getParameters() != null) {
            request.getParameters().forEach(source::addValue);
        }
        return source;
    }

    private int effectiveMaxResults(QueryRequest request) {
        int configMax = props.getQuery().getMaxResultsPerPage();
        return request.getMaxResults() > 0
                ? Math.min(request.getMaxResults(), configMax)
                : configMax;
    }
}
```

---

### ✅ What you should see when Step 6 is done
```
com/aws/utils/redshift/
└── connection/
    ├── RedshiftConnector.java
    ├── DataApiRedshiftConnector.java
    └── JdbcRedshiftConnector.java
```

Red errors in `RedshiftAutoConfiguration` should now be reduced to only these remaining missing classes:
```
RedshiftQueryExecutor       ← Step 7
RedshiftHealthIndicator     ← Step 8
CredentialsProviderFactory  ← Step 8
SecretsManagerUtil          ← Step 8
