package com.prephub.storage;

import com.prephub.config.AppProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minio;
    private final AppProperties props;

    public String uploadAvatar(UUID userId, MultipartFile file) throws Exception {
        String ext = extractExtension(file.getOriginalFilename());
        String key = "user-" + userId + "/" + UUID.randomUUID() + ext;
        try (var in = file.getInputStream()) {
            minio.putObject(PutObjectArgs.builder()
                    .bucket(props.storage().avatarBucket())
                    .object(key)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }
        return key;
    }

    public String publicUrl(String bucket, String key) {
        return props.storage().publicUrl() + "/" + bucket + "/" + key;
    }

    public String presignedGet(String bucket, String key, int expirySeconds) throws Exception {
        return minio.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET).bucket(bucket).object(key)
                .expiry(expirySeconds, TimeUnit.SECONDS).build());
    }

    public void delete(String bucket, String key) {
        try {
            minio.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            log.warn("Failed to delete object {}/{}: {}", bucket, key, e.getMessage());
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : "";
    }
}
