package com.storage.app.util.resource.file;

import com.storage.app.config.MinioConfig;
import com.storage.app.exception.FileBigSizeException;
import com.storage.app.exception.FileExistException;
import com.storage.app.exception.FileNotExistsException;
import com.storage.app.util.resource.folder.FolderChecker;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;

@Component
@Data
@Slf4j
public class FileChecker {

    private final FolderChecker folderChecker;
    private final MinioConfig minioConfig;

    public void checkFileSize(MultipartFile file) {
        if (file.getSize() > minioConfig.getMaxSize()) {
            throw new FileBigSizeException("File is too large");
        }
    }

    public void fileIsEmpty(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileNotExistsException("File was not found");
        }
    }

    public void fileExistsInDirectory(MinioClient minioClient, MultipartFile file, String path) throws Exception {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .prefix(path)
                        .recursive(false)
                        .build()
        );
        for (Result<Item> result : results) {
            Item item = result.get();
            String fileName = Paths.get(item.objectName()).getFileName().toString();
            String nameFrom = file.getOriginalFilename();
            if (nameFrom != null && nameFrom.equals(fileName)) {
                throw new FileExistException("File with name '" + nameFrom + "' already exists");
            }
        }
    }

}
