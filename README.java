// Add these imports
import com.edp.api.exception.InvalidTemplateException;
import com.edp.api.exception.InvalidColumnPresetException;
import com.edp.api.exception.InvalidFilterParamException;

// Add these handlers

@ExceptionHandler(InvalidTemplateException.class)
public ResponseEntity<Map<String, Object>>
        handleInvalidTemplate(
                InvalidTemplateException ex) {
    String traceId = UUID.randomUUID().toString();
    log.warn("[ExceptionHandler] Invalid template. " +
            "traceId={}, error={}",
            traceId, ex.getMessage());
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorBody(traceId, 400,
                    "BAD_REQUEST", ex.getMessage()));
}

@ExceptionHandler(InvalidColumnPresetException.class)
public ResponseEntity<Map<String, Object>>
        handleInvalidColumnPreset(
                InvalidColumnPresetException ex) {
    String traceId = UUID.randomUUID().toString();
    log.warn("[ExceptionHandler] Invalid column preset. " +
            "traceId={}, error={}",
            traceId, ex.getMessage());
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorBody(traceId, 400,
                    "BAD_REQUEST", ex.getMessage()));
}

@ExceptionHandler(InvalidFilterParamException.class)
public ResponseEntity<Map<String, Object>>
        handleInvalidFilterParam(
                InvalidFilterParamException ex) {
    String traceId = UUID.randomUUID().toString();
    log.warn("[ExceptionHandler] Invalid filter param. " +
            "traceId={}, error={}",
            traceId, ex.getMessage());
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorBody(traceId, 400,
                    "BAD_REQUEST", ex.getMessage()));
}
