@ExceptionHandler(TableNotFoundException.class)
public ResponseEntity<Map<String, Object>> handleTableNotFound(
        TableNotFoundException ex) {
    String traceId = UUID.randomUUID().toString();
    log.warn("[ExceptionHandler] Table not found. " +
            "traceId={}, error={}",
            traceId, ex.getMessage());
    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorBody(
                    traceId,
                    404,
                    "NOT_FOUND",
                    ex.getMessage()));
}
