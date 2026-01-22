package org.example.service;

import io.minio.*;
import io.minio.errors.*;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.example.config.MinIOConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@Stateless
public class MinIOService {

    @Inject
    private MinIOConfig minIOConfig;

    /**
     * Инициализация bucket'а, если он не существует
     */
    public void initializeBucket() throws Exception {
        try {
            MinioClient client = minIOConfig.getMinioClient();
            String bucketName = minIOConfig.getBucketName();
            
            boolean bucketExists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
                    
            if (!bucketExists) {
                log.info("Creating bucket: {}", bucketName);
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("Bucket created successfully: {}", bucketName);
            } else {
                log.debug("Bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize bucket: {}", minIOConfig.getBucketName(), e);
            throw new RuntimeException("Failed to initialize MinIO bucket", e);
        }
    }

    /**
     * Загрузка файла в MinIO
     */
    public FileUploadResult uploadFile(String filename, String contentType, byte[] fileContent) throws Exception {
        initializeBucket();
        
        String fileKey = generateFileKey(filename);
        MinioClient client = minIOConfig.getMinioClient();
        String bucketName = minIOConfig.getBucketName();
        
        try (InputStream inputStream = new ByteArrayInputStream(fileContent)) {
            log.info("Uploading file to MinIO: bucket={}, key={}, size={} bytes", 
                    bucketName, fileKey, fileContent.length);
            
            ObjectWriteResponse response = client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileKey)
                            .stream(inputStream, fileContent.length, -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );
            
            log.info("File uploaded successfully: etag={}, key={}", response.etag(), fileKey);
            
            return new FileUploadResult(fileKey, fileContent.length, contentType, response.etag());
            
        } catch (Exception e) {
            log.error("Failed to upload file: key={}", fileKey, e);
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    /**
     * Скачивание файла из MinIO
     */
    public FileDownloadResult downloadFile(String fileKey) throws Exception {
        MinioClient client = minIOConfig.getMinioClient();
        String bucketName = minIOConfig.getBucketName();
        
        try {
            log.info("Downloading file from MinIO: bucket={}, key={}", bucketName, fileKey);
            
            // Получаем информацию об объекте
            StatObjectResponse objectStat = client.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileKey)
                            .build()
            );
            
            // Скачиваем объект
            GetObjectResponse response = client.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileKey)
                            .build()
            );
            
            byte[] fileContent = response.readAllBytes();
            response.close();
            
            log.info("File downloaded successfully: key={}, size={} bytes", fileKey, fileContent.length);
            
            return new FileDownloadResult(
                    fileKey,
                    fileContent,
                    objectStat.contentType(),
                    objectStat.size(),
                    objectStat.lastModified()
            );
            
        } catch (Exception e) {
            log.error("Failed to download file: key={}", fileKey, e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }

    /**
     * Удаление файла из MinIO
     */
    public void deleteFile(String fileKey) throws Exception {
        MinioClient client = minIOConfig.getMinioClient();
        String bucketName = minIOConfig.getBucketName();
        
        try {
            log.info("Deleting file from MinIO: bucket={}, key={}", bucketName, fileKey);
            
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileKey)
                            .build()
            );
            
            log.info("File deleted successfully: key={}", fileKey);
            
        } catch (Exception e) {
            log.error("Failed to delete file: key={}", fileKey, e);
            throw new RuntimeException("Failed to delete file from MinIO", e);
        }
    }

    /**
     * Проверка существования файла
     */
    public boolean fileExists(String fileKey) {
        try {
            MinioClient client = minIOConfig.getMinioClient();
            String bucketName = minIOConfig.getBucketName();
            
            client.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileKey)
                            .build()
            );
            
            return true;
        } catch (Exception e) {
            log.debug("File does not exist or error checking: key={}", fileKey);
            return false;
        }
    }

    /**
     * Генерация уникального ключа для файла
     */
    private String generateFileKey(String originalFilename) {
        String timestamp = ZonedDateTime.now().toString().replaceAll("[^0-9]", "");
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        // Извлекаем расширение файла
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        return String.format("imports/%s_%s_%s%s", timestamp, uuid, 
                originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "file", 
                extension);
    }

    /**
     * Результат загрузки файла
     */
    public static record FileUploadResult(
            String fileKey,
            long fileSize,
            String contentType,
            String etag
    ) {}

    /**
     * Результат скачивания файла
     */
    public static record FileDownloadResult(
            String fileKey,
            byte[] content,
            String contentType,
            long size,
            ZonedDateTime lastModified
    ) {}
}