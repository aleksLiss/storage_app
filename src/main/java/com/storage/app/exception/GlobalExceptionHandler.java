package com.storage.app.exception;

import com.storage.app.exception.mapper.AnswerResponseDtoMapperException;
import com.storage.app.exception.resource.DeleteResourceException;
import com.storage.app.exception.resource.ResourceAlreadyExistsException;
import com.storage.app.exception.resource.ResourceNotFoundException;
import com.storage.app.exception.resource.ResourceUploadException;
import com.storage.app.exception.resource.file.FileBigSizeException;
import com.storage.app.exception.resource.file.FileDownloadException;
import com.storage.app.exception.resource.file.FileExistException;
import com.storage.app.exception.resource.file.FileNotExistsException;
import com.storage.app.exception.resource.folder.FolderCreateException;
import com.storage.app.exception.resource.folder.FolderDownloadException;
import com.storage.app.exception.resource.folder.FolderNotExistsException;
import com.storage.app.exception.user.UserAlreadyExistsException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ResourceAlreadyExistsException.class,
            FileExistException.class})
    public ResponseEntity<@NonNull Map<String, String>> handleResourceExistsExceptions(Exception ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<@NonNull Map<String, String>> handleUserExistsExceptions(UserAlreadyExistsException ex) {
        log.warn(ex.getMessage());
        String msg = "User already exists";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", msg));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<@NonNull Map<String, String>> handleValidResourcesExceptions(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors();
        String errorMessage = errors.stream()
                .filter(f -> f.getField().equals("username"))
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse(errors.get(0).getDefaultMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", errorMessage));
    }

    @ExceptionHandler({
            FolderNotExistsException.class,
            FileNotExistsException.class,
            ResourceNotFoundException.class})
    public ResponseEntity<@NonNull Map<String, String>> handleNotExistsException(Exception ex) {
        log.warn(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<@NonNull Map<String, String>> handleBadCredentials() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Incorrect username or password"));
    }

    @ExceptionHandler(FileBigSizeException.class)
    public ResponseEntity<@NonNull Map<String, String>> handleFileBigSizeException(FileBigSizeException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({
            AnswerResponseDtoMapperException.class, Exception.class,
            FolderCreateException.class,
            ResourceUploadException.class, DeleteResourceException.class,
            FolderDownloadException.class, FileDownloadException.class})
    public ResponseEntity<@NonNull Map<String, String>> handleGeneralException(Exception ex) {
        log.warn(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal Server Error"));
    }
}
