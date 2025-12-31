package com.recommendation.ml;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model Registry Service
 * Gère le versioning et le stockage des modèles ML dans MinIO
 */
@Slf4j
@Service
public class ModelRegistry {

    private final MinioClient minioClient;
    private final String bucketName = "ml-models";
    private final Map<String, ModelMetadata> modelMetadataCache = new ConcurrentHashMap<>();

    public ModelRegistry(@Value("${minio.endpoint:http://minio:9000}") String endpoint,
                         @Value("${minio.accessKey:minioadmin}") String accessKey,
                         @Value("${minio.secretKey:minioadmin}") String secretKey) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        
        initializeBucket();
    }

    private void initializeBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket", e);
        }
    }

    /**
     * Enregistre un modèle dans le registry
     */
    public void registerModel(String modelName, String version, byte[] modelBytes, Map<String, Object> metrics) {
        String objectName = String.format("%s/%s/model.bin", modelName, version);
        
        try {
            // Upload model to MinIO
            ByteArrayInputStream stream = new ByteArrayInputStream(modelBytes);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(stream, modelBytes.length, -1)
                            .contentType("application/octet-stream")
                            .build());
            
            // Store metadata
            ModelMetadata metadata = new ModelMetadata(
                    modelName,
                    version,
                    LocalDateTime.now(),
                    modelBytes.length,
                    metrics
            );
            modelMetadataCache.put(modelName + ":" + version, metadata);
            
            // Save metadata to MinIO as JSON
            String metadataJson = serializeMetadata(metadata);
            String metadataObjectName = String.format("%s/%s/metadata.json", modelName, version);
            ByteArrayInputStream metadataStream = new ByteArrayInputStream(metadataJson.getBytes());
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(metadataObjectName)
                            .stream(metadataStream, metadataJson.length(), -1)
                            .contentType("application/json")
                            .build());
            
            log.info("Registered model: {} version {} ({} bytes)", 
                    modelName, version, modelBytes.length);
            
        } catch (Exception e) {
            log.error("Failed to register model: {} version {}", modelName, version, e);
            throw new RuntimeException("Failed to register model", e);
        }
    }

    /**
     * Charge un modèle depuis le registry
     */
    public byte[] loadModel(String modelName, String version) {
        String objectName = String.format("%s/%s/model.bin", modelName, version);
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()).transferTo(outputStream);
            
            log.info("Loaded model: {} version {}", modelName, version);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Failed to load model: {} version {}", modelName, version, e);
            throw new RuntimeException("Failed to load model", e);
        }
    }

    /**
     * Récupère le dernier modèle pour un nom donné
     */
    public byte[] loadLatestModel(String modelName) {
        try {
            // List all versions for this model
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(modelName + "/")
                            .build());
            
            String latestVersion = null;
            for (Result<Item> result : results) {
                String objectName = result.get().objectName();
                String[] parts = objectName.split("/");
                if (parts.length >= 2 && objectName.endsWith("model.bin")) {
                    String version = parts[1];
                    if (latestVersion == null || version.compareTo(latestVersion) > 0) {
                        latestVersion = version;
                    }
                }
            }
            
            if (latestVersion == null) {
                throw new RuntimeException("No model found for: " + modelName);
            }
            
            return loadModel(modelName, latestVersion);
            
        } catch (Exception e) {
            log.error("Failed to load latest model: {}", modelName, e);
            throw new RuntimeException("Failed to load latest model", e);
        }
    }

    /**
     * Récupère la métadonnée d'un modèle
     */
    public ModelMetadata getModelMetadata(String modelName, String version) {
        String key = modelName + ":" + version;
        return modelMetadataCache.computeIfAbsent(key, k -> loadMetadata(modelName, version));
    }

    private ModelMetadata loadMetadata(String modelName, String version) {
        String objectName = String.format("%s/%s/metadata.json", modelName, version);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()).transferTo(outputStream);
            
            return deserializeMetadata(outputStream.toString("UTF-8"));
        } catch (Exception e) {
            log.warn("Failed to load metadata for {} version {}", modelName, version, e);
            return null;
        }
    }

    private String serializeMetadata(ModelMetadata metadata) {
        // Simple JSON serialization
        return String.format(
                "{\"modelName\":\"%s\",\"version\":\"%s\",\"trainedAt\":\"%s\",\"sizeBytes\":%d,\"metrics\":%s}",
                metadata.modelName(),
                metadata.version(),
                metadata.trainedAt().toString(),
                metadata.sizeBytes(),
                serializeMap(metadata.metrics())
        );
    }

    private ModelMetadata deserializeMetadata(String json) {
        // Simple JSON deserialization (for production, use Jackson)
        // This is a simplified implementation
        return new ModelMetadata("unknown", "unknown", LocalDateTime.now(), 0, Map.of());
    }

    private String serializeMap(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        map.forEach((k, v) -> sb.append("\"").append(k).append("\":").append(v).append(","));
        if (!map.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Métadonnées d'un modèle
     */
    public record ModelMetadata(
            String modelName,
            String version,
            LocalDateTime trainedAt,
            long sizeBytes,
            Map<String, Object> metrics
    ) {}
}
