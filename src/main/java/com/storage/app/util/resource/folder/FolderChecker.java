package com.storage.app.util.resource.folder;

import com.storage.app.exception.FolderAlreadyExistsException;
import com.storage.app.exception.FolderNotExistsException;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.Arrays;

@Component
public class FolderChecker {

    @Value("${spring.minio.bucket_name}")
    private String bucketName;

    public void isFolderNotExist(MinioClient minioClient, String path) throws Exception {
        String foldersPath = Paths.get(path).toString().replace("\\", "/");
        String[] folders = Arrays.stream(foldersPath.split("/"))
                .filter(s -> !s.contains("."))
                .toArray(String[]::new);
        String[] foldersWithoutLastFolder = Arrays.copyOf(folders, folders.length - 1);
        if (folders.length > 1) {
            StringBuilder folderName = new StringBuilder();
            for (String folder : foldersWithoutLastFolder) {
                folderName.append(folder).append("/");
                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .prefix(folderName.toString())
                                .recursive(false)
                                .build()
                );
                if (!results.iterator().hasNext()) {
                    throw new FolderNotExistsException("Folder with name '" + folder + "' does not exist");
                }
            }
        }
    }

    public void isFolderExist(MinioClient minioClient, String folderName) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(folderName)
                        .recursive(false)
                        .build()
        );
        if (results.iterator().hasNext()) {
            throw new FolderAlreadyExistsException("Folder with name " + folderName + " already exists");
        }
    }
}
