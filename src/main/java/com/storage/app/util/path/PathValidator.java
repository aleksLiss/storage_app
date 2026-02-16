package com.storage.app.util.path;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Component
public class PathValidator {

    public void validatePaths(String pathFromStr, String pathToStr) {
        if (pathFromStr == null || pathToStr == null || pathFromStr.isEmpty() || pathToStr.isEmpty()) {
            throw new IllegalArgumentException("400: Paths cannot be empty");
        }
        Path pathFrom = Paths.get(pathFromStr).normalize();
        Path pathTo = Paths.get(pathToStr).normalize();
        if (pathFrom.equals(pathTo)) {
            throw new IllegalArgumentException("400: Source and destination paths are the same");
        }
        if (pathTo.startsWith(pathFrom)) {
            throw new IllegalArgumentException("400: Destination path cannot be a sub-path of the source path");
        }
        String lastFrom = pathFrom.getFileName().toString();
        String lastTo = pathTo.getFileName().toString();
        boolean isFromFile = isFile(lastFrom);
        boolean isToFile = isFile(lastTo);
        if (!isFromFile && isToFile) {
            throw new IllegalArgumentException("400: Cannot copy directory to a file path");
        }
        if (isFromFile && isToFile) {
            if (!lastFrom.equals(lastTo)) {
                if (!Objects.equals(pathFrom.getParent(), pathTo.getParent())) {
                    throw new IllegalArgumentException("400: Invalid rename and diff length path");
                }
            }
        }
    }

    private static boolean isFile(String name) {
        return name.contains(".");
    }
}
