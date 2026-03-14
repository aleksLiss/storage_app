package com.storage.app.util.file;

import com.storage.app.config.MinioProperties;
import com.storage.app.exception.resource.file.FileBigSizeException;
import com.storage.app.exception.resource.file.FileExistException;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;

@Component
@Slf4j
@RequiredArgsConstructor
public class FileChecker {

    private final MinioProperties minioProperties;

    public void checkFileSize(MultipartFile file) {
        if (file.getSize() > minioProperties.maxSizeFile()) {
            throw new FileBigSizeException("File is too large");
        }
    }

    public void fileExistsInDirectory(MinioClient minioClient, MultipartFile file, String path) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(minioProperties.bucketName())
                        .prefix(path)
                        .recursive(false)
                        .build()
        );
        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                String fileName = Paths.get(item.objectName()).getFileName().toString();
                String nameFrom = file.getOriginalFilename();
                if (nameFrom != null && nameFrom.equals(fileName)) {
                    throw new FileExistException("File with name '" + fileName + "' already exists");
                }
            }
        } catch (FileExistException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error while checking file existence", ex);
            throw new RuntimeException("Storage error", ex);
        }
    }
}
