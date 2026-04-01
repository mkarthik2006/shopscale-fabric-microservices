package com.shopscale.common.exception;

import com.shopscale.common.dto.StandardResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardResponse<Object>> handleException(Exception ex) {
        log.error("Unhandled Server Exception", ex);
        return ResponseEntity.internalServerError()
                .body(StandardResponse.failure("Internal server error", 500));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource Not Found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(StandardResponse.failure(ex.getMessage(), 404));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<StandardResponse<Object>> handleBusinessException(BusinessException ex) {
        log.warn("Business Exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(StandardResponse.failure(ex.getMessage(), 422));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Payload Validation Failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(StandardResponse.failure(errors, 400));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StandardResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));

        log.warn("Constraint Violation: {}", errors);
        return ResponseEntity.badRequest()
                .body(StandardResponse.failure(errors, 400));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardResponse<Object>> handleJsonError(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON received: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(StandardResponse.failure("Invalid JSON format or missing required fields", 400));
    }
}
