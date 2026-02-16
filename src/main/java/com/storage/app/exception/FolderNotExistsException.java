package com.storage.app.exception;

public class FolderNotExistsException extends RuntimeException {
    public FolderNotExistsException(String message) {
        super(message);
    }
}
