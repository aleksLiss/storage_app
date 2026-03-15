package com.storage.app.util.file;

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

@Slf4j
@Component
public class FileChecker {

    public static void checkFileSize(MultipartFile file, long maxFileSize) {
        if (file.getSize() > maxFileSize) {
            throw new FileBigSizeException("File is too large");
        }
    }

    public static void fileExistsInDirectory(MinioClient minioClient, MultipartFile file, String path, String bucketName) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
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
