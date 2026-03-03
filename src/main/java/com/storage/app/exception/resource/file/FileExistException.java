package com.storage.app.exception.resource.file;

public class FileExistException extends RuntimeException {
    public FileExistException(String message) {
        super(message);
    }
}
