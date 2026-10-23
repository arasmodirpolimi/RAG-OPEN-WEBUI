package com.ragplatform.api.web;

import com.ragplatform.domain.exception.DocumentNotFoundException;
import com.ragplatform.domain.exception.DuplicateDocumentException;
import com.ragplatform.domain.exception.InvalidStateTransitionException;
import com.ragplatform.domain.exception.UnknownCollectionException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain exceptions to HTTP responses for the document API. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DuplicateDocumentException.class)
    ResponseEntity<Map<String, String>> duplicate(DuplicateDocumentException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage(), "existingId", e.existingId().toString()));
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(DocumentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler({UnknownCollectionException.class, IllegalArgumentException.class})
    ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    ResponseEntity<Map<String, String>> conflict(InvalidStateTransitionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}
