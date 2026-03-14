package com.storage.app.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient getMinioClient(MinioProperties minioProperties) {
        return MinioClient.builder()
                .endpoint(minioProperties.endpoint())
                .credentials(minioProperties.credentials().login(), minioProperties.credentials().password())
                .build();
    }

    @Bean
    public CommandLineRunner isBucketExist(MinioClient minioClient, MinioProperties minioProperties) {
        return args -> {
            String bucketName = minioProperties.bucketName();
            try {
                boolean found = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucketName).build()
                );
                if (!found) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(bucketName).build()
                    );
                    log.info("Bucket '{}' created successfully", bucketName);
                }
            } catch (Exception e) {
                log.error("Failed to initialize MinIO bucket: {}", e.getMessage());
            }
        };
    }
}

