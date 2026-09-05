package com.craftbid.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler Error Suppression & Sanitization Tests")
public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should suppress raw database SQL and table names in DataIntegrityViolationException")
    void shouldSuppressRawDatabaseErrors() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement [Duplicate entry 'test@example.com' for key 'users.UK_email'] [insert into users (email, password) values (?, ?)]"
        );

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDatabaseExceptions(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        String message = (String) body.get("message");
        assertFalse(message.contains("Duplicate entry"));
        assertFalse(message.contains("UK_email"));
        assertFalse(message.contains("insert into users"));
        assertFalse(message.contains("users."));
        assertEquals("A database conflict or constraint violation occurred. Please verify your data and try again.", message);
    }

    @Test
    @DisplayName("Should suppress raw SQL exceptions")
    void shouldSuppressSqlExceptions() {
        SQLException ex = new SQLException("Table 'craftbid_db.users' doesn't exist", "42S02", 1146);

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDatabaseExceptions(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse(((String) body.get("message")).contains("craftbid_db.users"));
        assertFalse(((String) body.get("message")).contains("42S02"));
    }

    @Test
    @DisplayName("Should suppress local filesystem paths in IOException")
    void shouldSuppressFileSystemPaths() {
        FileSystemException ex = new FileSystemException(
                "/Users/ranjitbarik/CraftBid/uploads/crafts/secret-image.png",
                null,
                "Permission denied"
        );

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleFileStorageExceptions(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        String message = (String) body.get("message");
        assertFalse(message.contains("/Users/ranjitbarik"));
        assertFalse(message.contains("secret-image.png"));
        assertFalse(message.contains("uploads/crafts"));
        assertEquals("Failed to process or store the uploaded file. Please try again.", message);
    }

    @Test
    @DisplayName("Should sanitize technical RuntimeException containing class names or stack paths")
    void shouldSanitizeTechnicalRuntimeExceptions() {
        RuntimeException ex = new RuntimeException(
                "java.lang.NullPointerException: Cannot invoke \"com.craftbid.entity.User.getId()\" because \"user\" is null at com.craftbid.service.CraftService.uploadCraft(CraftService.java:140)"
        );

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleRuntimeException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        String message = (String) body.get("message");
        assertFalse(message.contains("NullPointerException"));
        assertFalse(message.contains("com.craftbid"));
        assertFalse(message.contains("CraftService.java"));
        assertEquals("An internal error occurred while processing your request. Please try again.", message);
    }

    @Test
    @DisplayName("Should preserve safe business validation messages in RuntimeException")
    void shouldPreserveSafeBusinessExceptions() {
        RuntimeException ex = new RuntimeException("Invalid email or password");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleRuntimeException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid email or password", body.get("message"));
    }

    @Test
    @DisplayName("Should handle AccessDeniedException without exposing internal security context")
    void shouldHandleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleAccessDenied(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Access denied. You do not have permission to perform this action.", body.get("message"));
    }

    @Test
    @DisplayName("Should handle BadCredentialsException cleanly")
    void shouldHandleBadCredentials() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleAuthenticationException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid authentication credentials.", body.get("message"));
    }

    @Test
    @DisplayName("Should return generic error for unhandled top-level Throwable")
    void shouldHandleGenericThrowable() {
        Throwable ex = new OutOfMemoryError("Java heap space");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericThrowable(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse(((String) body.get("message")).contains("Java heap space"));
        assertEquals("An unexpected error occurred. Please try again later.", body.get("message"));
    }
}
