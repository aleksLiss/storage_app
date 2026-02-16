package com.storage.app.exception;

public class FileNotExistsException extends RuntimeException {
    public FileNotExistsException(String message) {
        super(message);
    }
}
