package com.safenavi.backend.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record S3Properties(S3 s3, Credentials credentials) {

    public record S3(String bucket, String region, String publicBaseUrl) {
    }

    /** Both empty -> the SDK default credential chain is used. Values come from environment variables only. */
    public record Credentials(String accessKey, String secretKey) {

        public boolean isConfigured() {
            return accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank();
        }
    }
}
