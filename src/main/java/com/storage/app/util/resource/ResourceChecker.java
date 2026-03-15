package com.storage.app.util.resource;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResourceChecker {

    public static boolean isResourceExists(String resource, String bucketName, MinioClient minioClient) {
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
