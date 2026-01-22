package org.example.config;

import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@Startup
public class MinIOConfig {

    private MinioClient minioClient;
    private String bucketName = "import-files";
    
    // Конфигурация MinIO
    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin123";

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing MinIO client with endpoint: {}", endpoint);
            
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
                    
            log.info("MinIO client initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize MinIO client", e);
            throw new RuntimeException("MinIO client initialization failed", e);
        }
    }

    public MinioClient getMinioClient() {
        return minioClient;
    }

    public String getBucketName() {
        return bucketName;
    }
}