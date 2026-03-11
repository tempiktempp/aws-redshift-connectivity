package com.edp.api.service;

import com.aws.utils.redshift.executor.RedshiftQueryExecutor;
import com.aws.utils.redshift.model.QueryRequest;
import com.aws.utils.redshift.model.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedshiftDataService {

    private final RedshiftQueryExecutor queryExecutor;

    public List<Map<String, Object>> fetchTableData(
            String schemaName,
            String tableName,
            String status,
            int maxResults) {

        // Both schema and table are already pattern-validated
        // in the controller — only lowercase letters and
        // underscores allowed, safe to use as SQL identifiers
        String qualifiedTable = schemaName + "." + tableName;

        String sql = status != null
                ? "SELECT * FROM " + qualifiedTable +
                  " WHERE status = :status"
                : "SELECT * FROM " + qualifiedTable;

        QueryRequest.QueryRequestBuilder builder =
                QueryRequest.builder()
                        .sql(sql)
                        .maxResults(maxResults)
                        .queryLabel("fetch-" + qualifiedTable);

        if (status != null) {
            builder.parameter("status", status);
        }

        return queryExecutor.queryForList(builder.build());
    }

    public Map<String, Object> fetchById(
            String schemaName,
            String tableName,
            Object id) {

        String qualifiedTable = schemaName + "." + tableName;

        QueryRequest request = QueryRequest.builder()
                .sql("SELECT * FROM " + qualifiedTable +
                     " WHERE id = :id")
                .parameter("id", id)
                .maxResults(1)
                .queryLabel("fetch-by-id-" + qualifiedTable)
                .build();

        return queryExecutor.queryForSingleRow(request);
    }

    public QueryResult executeCustomQuery(
            String sql,
            Map<String, Object> parameters,
            int maxResults) {

        QueryRequest.QueryRequestBuilder builder =
                QueryRequest.builder()
                        .sql(sql)
                        .maxResults(maxResults)
                        .queryLabel("custom-query");

        if (parameters != null) {
            parameters.forEach(builder::parameter);
        }

        return queryExecutor.executeQuery(builder.build());
    }
}
```

---

### Your new endpoints
```
GET /api/v1/redshift/schemas/crm/tables/contact
GET /api/v1/redshift/schemas/crm/tables/contact?status=OPEN
GET /api/v1/redshift/schemas/crm/tables/contact?maxResults=50
GET /api/v1/redshift/schemas/crm/tables/contact/123
GET /api/v1/redshift/schemas/crm/tables/interaction
