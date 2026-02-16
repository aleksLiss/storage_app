package com.storage.app.exception;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<@NonNull Map<String, String>> handleFileExistException(ResourceAlreadyExistsException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({ResourceNotFoundException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<@NonNull Map<String, String>> handleFileExistException(Exception ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({FolderNotExistsException.class, FileNotExistsException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<@NonNull Map<String, String>> handleFolderNotExistsException(Exception ex) {
        log.warn(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({FileExistException.class, FolderAlreadyExistsException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<@NonNull Map<String, String>> handleFolderAlreadyExistsException(Exception ex) {
        Map<String, String> response = new HashMap<>();
        log.warn(ex.getMessage());
        response.put("message", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<@NonNull Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        log.error("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<@NonNull Map<String, String>> handleUserExists() {
        Map<String, String> response = new HashMap<>();
        log.warn("User already exists");
        response.put("message", "Username already exists");
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<@NonNull Map<String, String>> handleBadCredentials() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildBodyExceptionResponse(HttpStatus.UNAUTHORIZED));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<@NonNull Map<String, String>> handleGeneralException() {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildBodyExceptionResponse(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private Map<String, String> buildBodyExceptionResponse(HttpStatus status) {
        Map<String, String> response = new HashMap<>();
        switch (status) {
            case UNAUTHORIZED:
                log.warn("Unauthorized");
                response.put("message", "Incorrect login or password");
                break;
            case INTERNAL_SERVER_ERROR:
                log.warn("Internal server error");
                response.put("message", "Internal Server Error");
                break;
        }
        return response;
    }
}
