package com.storage.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.minio.storage.app")
public record MinioProperties(
        String bucketName,
        Credentials credentials,
        String endpoint,
        long maxSizeFile)
{
    public record Credentials(String login, String password) {}
}
