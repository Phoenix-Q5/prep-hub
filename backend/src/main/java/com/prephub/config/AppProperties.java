package com.prephub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Storage storage, Cache cache) {
    public record Jwt(String secret, long expirationMs, long refreshExpirationMs, String issuer) {}
    public record Cors(String allowedOrigins) {}
    public record Storage(String endpoint, String publicUrl, String accessKey, String secretKey,
                          String avatarBucket, String attachmentBucket) {}
    public record Cache(int searchTtlSeconds, int hotTopicsTtlSeconds) {}
}
