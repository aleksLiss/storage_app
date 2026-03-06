package com.storage.app.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Slf4j
public class MinioConfig {

    @Value("${spring.minio.bucket_name}")
    private String bucketName;
    @Value("${spring.minio.credentials.login}")
    private String login;
    @Value("${spring.minio.credentials.password}")
    private String password;
    @Value("${spring.minio.endpoint}")
    private String endpoint;
    @Value("${spring.minio.max_size_file}")
    private double maxSize;

    public MinioClient getMinioClient() {
        return MinioClient.builder()
                .endpoint(getEndpoint())
                .credentials(getLogin(), getPassword())
                .build();
    }

    @Bean
    public CommandLineRunner isBucketExist() {
        MinioClient minioClient = getMinioClient();
        return args -> {
            String bucketName = getBucketName();
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

