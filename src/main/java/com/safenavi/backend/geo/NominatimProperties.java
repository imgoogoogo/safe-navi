package com.safenavi.backend.geo;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geo.nominatim")
public record NominatimProperties(String baseUrl, String userAgent, Duration timeout) {
}
