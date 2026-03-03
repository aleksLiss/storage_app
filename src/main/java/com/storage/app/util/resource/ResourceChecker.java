package com.storage.app.util.resource;

import com.storage.app.config.MinioConfig;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Data
public class ResourceChecker {

    private final MinioConfig minioConfig;

    public boolean isResourceExists(String resource) {
        MinioClient client = minioConfig.getMinioClient();
        String bucketName = minioConfig.getBucketName();
        try {
            Iterable<Result<Item>> results = client.listObjects(
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
