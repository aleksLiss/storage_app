package com.storage.app.exception.resource.folder;

public class FolderNotExistsException extends RuntimeException {
    public FolderNotExistsException(String message) {
        super(message);
    }
}
