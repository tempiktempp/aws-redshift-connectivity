private QueryResult fetchResults(String queryId,
                                  QueryRequest request) {
    List<Map<String, Object>> allRows = new ArrayList<>();
    List<QueryResult.ColumnMetadata> columnMetadata = null;
    String nextToken = null;

    int maxResults = request.getMaxResults() > 0
            ? Math.min(request.getMaxResults(),
                       props.getQuery().getMaxResultsPerPage())
            : props.getQuery().getMaxResultsPerPage();

    log.debug("[Redshift-DataAPI] Fetching results, " +
            "maxResults cap: {}", maxResults);

    do {
        // Stop fetching pages if we already have enough rows
        if (allRows.size() >= maxResults) {
            log.debug("[Redshift-DataAPI] Row cap reached: {}",
                    maxResults);
            break;
        }

        GetStatementResultRequest.Builder resultBuilder =
                GetStatementResultRequest.builder()
                        .id(queryId);

        if (nextToken != null) {
            resultBuilder.nextToken(nextToken);
        }

        GetStatementResultResponse response =
                redshiftDataClient.getStatementResult(
                        resultBuilder.build());

        // Extract column metadata on first page only
        if (columnMetadata == null) {
            columnMetadata = extractColumnMetadata(
                    response.columnMetadata());
        }

        // Only add rows up to the maxResults cap
        List<QueryResult.ColumnMetadata> finalMeta =
                columnMetadata;

        for (List<Field> row : response.records()) {
            if (allRows.size() >= maxResults) {
                // Hit the cap mid-page — stop adding rows
                // and clear nextToken to stop pagination
                nextToken = null;
                break;
            }
            allRows.add(mapRow(row, finalMeta));
        }

        // Only update nextToken if we haven't hit the cap
        if (nextToken != null || allRows.size() < maxResults) {
            nextToken = response.nextToken();
        }

    } while (nextToken != null && !nextToken.isBlank());

    return QueryResult.builder()
            .rows(Collections.unmodifiableList(allRows))
            .columnMetadata(columnMetadata != null
                    ? columnMetadata
                    : Collections.emptyList())
            .totalRows(allRows.size())
            .queryId(queryId)
            .strategy(STRATEGY_NAME)
            .build();
}
