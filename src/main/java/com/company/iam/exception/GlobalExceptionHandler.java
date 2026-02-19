// exception/GlobalExceptionHandler.java
package com.company.iam.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IamException.class)
    public ResponseEntity<Map<String, Object>> handleIamException(IamException e) {
        log.error("IAM Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(buildError(
            e.getStatus().value(), e.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors()
            .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));

        Map<String, Object> response = buildError(400, "Validation failed");
        response.put("errors", errors);
        return ResponseEntity.badRequest().body(response);
    }

    private Map<String, Object> buildError(int status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", status);
        error.put("message", message);
        error.put("timestamp", LocalDateTime.now().toString());
        return error;
    }
}