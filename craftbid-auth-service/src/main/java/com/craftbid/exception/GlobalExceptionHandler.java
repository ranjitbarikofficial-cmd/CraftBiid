package com.craftbid.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler enforcing strict error suppression across all API endpoints.
 * Prevents information disclosure (stack traces, internal paths, raw database/SQL errors)
 * while logging comprehensive diagnostics server-side for maintainability and debugging.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Map<String, Object> buildErrorBody(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }

    /**
     * Inspects messages and exception types to identify technical or low-level leakage.
     */
    private boolean isTechnicalMessage(Throwable ex, String msg) {
        if (msg == null || msg.isBlank()) {
            return true;
        }
        if (ex instanceof NullPointerException || ex instanceof IndexOutOfBoundsException || ex instanceof ClassCastException) {
            return true;
        }
        String lower = msg.toLowerCase();
        return lower.contains("exception")
                || lower.contains("sql")
                || lower.contains("select ")
                || lower.contains("insert ")
                || lower.contains("update ")
                || lower.contains("delete ")
                || lower.contains("table ")
                || lower.contains("column ")
                || lower.contains("foreign key")
                || lower.contains("constraint")
                || lower.contains("hibernate")
                || lower.contains("org.springframework")
                || lower.contains("jakarta.")
                || lower.contains("com.mysql")
                || lower.contains("com.craftbid")
                || lower.contains("/users/")
                || lower.contains("/home/")
                || lower.contains("/app/")
                || lower.contains("c:\\")
                || lower.contains("at line")
                || lower.contains(".java:")
                || lower.contains("nullpointer");
    }

    private String sanitizeMessage(Throwable ex, String msg, String defaultMessage) {
        if (msg == null || msg.isBlank()) {
            return defaultMessage;
        }
        if (isTechnicalMessage(ex, msg)) {
            return defaultMessage;
        }
        return msg.trim();
    }

    // =========================================================================
    // DATABASE / PERSISTENCE EXCEPTIONS (No SQL / table / column names exposed)
    // =========================================================================

    @ExceptionHandler({DataIntegrityViolationException.class, DataAccessException.class, SQLException.class})
    public ResponseEntity<Map<String, Object>> handleDatabaseExceptions(Exception ex) {
        logger.error("Database operation error encountered: ", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorBody(HttpStatus.BAD_REQUEST, "A database conflict or constraint violation occurred. Please verify your data and try again."));
    }

    // =========================================================================
    // I/O & FILE STORAGE EXCEPTIONS (No server filesystem paths exposed)
    // =========================================================================

    @ExceptionHandler({IOException.class, FileSystemException.class})
    public ResponseEntity<Map<String, Object>> handleFileStorageExceptions(Exception ex) {
        logger.error("File storage or I/O failure: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process or store the uploaded file. Please try again."));
    }

    // =========================================================================
    // SECURITY & ACCESS CONTROL
    // =========================================================================

    @ExceptionHandler({AccessDeniedException.class, org.springframework.security.access.AccessDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDenied(Exception ex) {
        logger.warn("Access denied violation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildErrorBody(HttpStatus.FORBIDDEN, "Access denied. You do not have permission to perform this action."));
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorBody(HttpStatus.UNAUTHORIZED, "Invalid authentication credentials."));
    }

    // =========================================================================
    // SCHEMA & INPUT VALIDATION (Structured, Field-Level Errors)
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Map<String, Object> errorItem = new HashMap<>();
            errorItem.put("field", fieldError.getField());
            errorItem.put("rejectedValue", fieldError.getRejectedValue());
            errorItem.put("message", fieldError.getDefaultMessage());
            errors.add(errorItem);
        }

        String mainMessage = errors.isEmpty() ? "Validation failed" : (String) errors.get(0).get("message");
        logger.debug("Request schema validation rejected: {}", errors);

        Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, mainMessage);
        body.put("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, Object>> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            Map<String, Object> errorItem = new HashMap<>();
            errorItem.put("field", violation.getPropertyPath().toString());
            errorItem.put("rejectedValue", violation.getInvalidValue());
            errorItem.put("message", violation.getMessage());
            errors.add(errorItem);
        }

        String mainMessage = errors.isEmpty() ? "Validation failed" : (String) errors.get(0).get("message");
        logger.debug("URL/Param constraint violation rejected: {}", errors);

        Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, mainMessage);
        body.put("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    // =========================================================================
    // PAYLOAD / FORMAT / TYPE MISMATCH
    // =========================================================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        logger.warn("Malformed HTTP message received: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorBody(HttpStatus.BAD_REQUEST, "Malformed JSON request payload or invalid field format"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String typeName = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid type";
        String message = String.format("Parameter '%s' must be of type '%s'", ex.getName(), typeName);
        logger.warn("Method argument type mismatch: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorBody(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        logger.warn("Missing request parameter: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorBody(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        logger.warn("Max upload size exceeded: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(buildErrorBody(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds the maximum allowed upload limit"));
    }

    // =========================================================================
    // BUSINESS LOGIC & GENERAL RUNTIME EXCEPTIONS
    // =========================================================================

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(RuntimeException ex) {
        logger.warn("Business argument exception: {}", ex.getMessage());
        String safeMessage = sanitizeMessage(ex, ex.getMessage(), "Invalid request parameters.");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorBody(HttpStatus.BAD_REQUEST, safeMessage));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        String rawMessage = ex.getMessage();
        if (isTechnicalMessage(ex, rawMessage)) {
            logger.error("Internal runtime exception: ", ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred while processing your request. Please try again."));
        }

        logger.warn("Runtime business exception: {}", rawMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorBody(HttpStatus.BAD_REQUEST, sanitizeMessage(ex, rawMessage, "Invalid request.")));
    }

    // =========================================================================
    // UNHANDLED SYSTEM EXCEPTIONS / CATCH-ALL (No stack traces to clients)
    // =========================================================================

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> handleGenericThrowable(Throwable ex) {
        logger.error("Unhandled top-level error: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later."));
    }
}