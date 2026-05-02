package com.prephub.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Slf4j
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(AppProperties props) {
        var s = props.storage();
        return MinioClient.builder()
                .endpoint(s.endpoint())
                .credentials(s.accessKey(), s.secretKey())
                .build();
    }

    @Component
    @RequiredArgsConstructor
    static class BucketInitializer {
        private final MinioClient client;
        private final AppProperties props;

        @PostConstruct
        void init() {
            ensureBucket(props.storage().avatarBucket());
            ensureBucket(props.storage().attachmentBucket());
        }

        private void ensureBucket(String bucket) {
            try {
                if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Created bucket: {}", bucket);
                }
            } catch (Exception e) {
                log.warn("Could not initialize bucket {}: {}", bucket, e.getMessage());
            }
        }
    }
}
