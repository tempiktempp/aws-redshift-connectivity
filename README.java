private String buildSelectClause(
        ColumnPreset columnPreset) {

    // If preset contains wildcard — generate SELECT *
    if (columnPreset.getColumns().contains("*")) {
        return "*";
    }

    return columnPreset.getColumns()
            .stream()
            .sorted()
            .collect(Collectors.joining(", "));
}
