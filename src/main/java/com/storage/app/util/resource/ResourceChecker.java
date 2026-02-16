package com.storage.app.util.resource;

import com.storage.app.exception.ResourceAlreadyExistsException;
import com.storage.app.exception.ResourceNotFoundException;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.stereotype.Component;

@Component
public class ResourceChecker {

    public void checkResourceNotFound(Iterable<Result<Item>> results) {
        if (!results.iterator().hasNext()) {
            throw new ResourceNotFoundException("Resource not found");
        }
    }

    public void checkResourceAlreadyExists(String resource, Iterable<Result<Item>> results) {
        if (resource.contains(".") && results.iterator().hasNext()) {
            throw new ResourceAlreadyExistsException("Resource already exists");
        }
    }
}
