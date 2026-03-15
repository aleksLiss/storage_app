package com.storage.app.util.resource;

import org.springframework.stereotype.Component;

@Component
public class ResourceFinder {

    public static String getResourceNameFromPath(String fullPath) {
        String[] path = fullPath.split("/");
        return path[path.length - 1];
    }
}
