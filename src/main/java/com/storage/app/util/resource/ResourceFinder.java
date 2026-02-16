package com.storage.app.util.resource;

import org.springframework.stereotype.Component;

@Component
public class ResourceFinder {

    public String getResourceName(String fullPath) {
        String[] path = fullPath.split("/");
        return path[path.length - 1];
    }

    public String getPathToResource(String fullPath) {
        StringBuilder pathBuilder = new StringBuilder();
        String[] path = fullPath.split("/");
        for (int i = 0; i < path.length - 1; i++) {
            pathBuilder.append(path[i]);
            pathBuilder.append("/");
        }
        return pathBuilder.toString();
    }
}
