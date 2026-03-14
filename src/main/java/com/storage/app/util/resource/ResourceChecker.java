package com.storage.app.util.resource;

import com.storage.app.config.MinioProperties;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceChecker {

    private final MinioProperties minioProperties;
    private final MinioClient minioClient;

    public boolean isResourceExists(String resource) {
        String bucketName = minioProperties.bucketName();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(resource)
                            .maxKeys(1)
                            .build());
            return results.iterator().hasNext();
        } catch (Exception ex) {
            String msg = "Exception during check is resource exist";
            log.error(msg);
            throw new RuntimeException(msg);
        }
    }
}
