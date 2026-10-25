package com.safenavi.backend.geo;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geo.kakao")
public record KakaoLocalProperties(String baseUrl, String restApiKey, Duration timeout) {

    public KakaoLocalProperties {
        if (timeout == null) {
            timeout = Duration.ofSeconds(5);
        }
    }
}
