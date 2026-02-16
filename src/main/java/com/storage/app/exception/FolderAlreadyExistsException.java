package com.storage.app.exception;

public class FolderAlreadyExistsException extends RuntimeException {
    public FolderAlreadyExistsException(String message) {
        super(message);
    }
}
