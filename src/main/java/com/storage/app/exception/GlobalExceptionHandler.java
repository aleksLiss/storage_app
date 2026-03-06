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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ResourceAlreadyExistsException.class,
            FolderCreateException.class,
            UserAlreadyExistsException.class,
            FileExistException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<@NonNull Map<String, String>> handleExistsExceptions(Exception ex) {
        if (ex instanceof UserAlreadyExistsException) {
            log.warn(ex.getMessage());
            String msg = "User already exists";
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", msg));
        }
        log.warn(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<@NonNull Map<String, String>> handleValidResourcesExceptions(MethodArgumentNotValidException ex) {
        var fieldErrors = ex.getBindingResult().getFieldErrors();
        var priorityError = fieldErrors.stream()
                .filter(f -> f.getField().equals("username"))
                .findFirst()
                .orElseGet(() -> fieldErrors.stream().findFirst().orElse(null));
        if (priorityError == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Ошибка валидации"));
        }
        Map<String, String> response = new HashMap<>();
        response.put("field", priorityError.getField());
        response.put("message", priorityError.getDefaultMessage());
        response.put(priorityError.getField(), priorityError.getDefaultMessage());
        log.warn("Validation error in field [{}]: {}", priorityError.getField(), priorityError.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({
            FolderNotExistsException.class,
            FileNotExistsException.class,
            ResourceNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<@NonNull Map<String, String>> handleNotExistsException(Exception ex) {
        log.warn(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<@NonNull Map<String, String>> handleBadCredentials() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Incorrect username or password"));
    }

    @ExceptionHandler({
            AnswerResponseDtoMapperException.class, Exception.class,
            ResourceUploadException.class, DeleteResourceException.class,
            FolderDownloadException.class, FileDownloadException.class,
            FileBigSizeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<@NonNull Map<String, String>> handleGeneralException(Exception ex) {
        log.warn(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal Server Error"));
    }
}
