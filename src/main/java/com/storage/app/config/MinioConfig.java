package com.storage.app.config;

import io.minio.MinioClient;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
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
}
