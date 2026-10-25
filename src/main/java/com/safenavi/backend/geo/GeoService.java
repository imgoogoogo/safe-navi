package com.safenavi.backend.geo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.common.geo.Coordinate;

import tools.jackson.databind.JsonNode;

/**
 * Reverse geocoding through OSM Nominatim. The public instance allows at most one request per
 * second and requires an identifying User-Agent, so calls are serialized and results cached.
 */
@Service
public class GeoService {

    private static final Logger log = LoggerFactory.getLogger(GeoService.class);

    private static final long MIN_INTERVAL_MILLIS = 1_000;
    private static final int CACHE_SIZE = 2_000;
    /** Address keys Nominatim uses for Korean 동/읍/면, most specific first. */
    private static final List<String> DONG_KEYS = List.of("quarter", "suburb", "neighbourhood", "village", "town");

    private final WebClient webClient;
    private final NominatimProperties properties;
    private final Map<String, ReverseGeocodeResponse> cache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ReverseGeocodeResponse> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    private long lastCallAt;

    public GeoService(NominatimProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ko")
                .build();
    }

    /** @throws BusinessException EXTERNAL_API_ERROR when Nominatim fails or has no result */
    public ReverseGeocodeResponse reverse(Coordinate point) {
        String key = String.format("%.5f,%.5f", point.lat(), point.lng());
        synchronized (cache) {
            ReverseGeocodeResponse cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
        }
        JsonNode body = call(point);
        if (body == null || body.has("error")) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "해당 좌표의 주소를 찾을 수 없습니다.");
        }
        ReverseGeocodeResponse result = map(body.path("address"));
        synchronized (cache) {
            cache.put(key, result);
        }
        return result;
    }

    /** For callers where an address is optional: never throws. */
    public Optional<String> findAddress(Coordinate point) {
        try {
            ReverseGeocodeResponse r = reverse(point);
            return Optional.ofNullable(r.roadAddress() != null ? r.roadAddress() : r.dong());
        } catch (RuntimeException e) {
            log.warn("Reverse geocoding failed for {}: {}", point, e.getMessage());
            return Optional.empty();
        }
    }

    private JsonNode call(Coordinate point) {
        throttle();
        try {
            return webClient.get()
                    .uri(uri -> uri.path("/reverse")
                            .queryParam("format", "jsonv2")
                            .queryParam("lat", point.lat())
                            .queryParam("lon", point.lng())
                            .queryParam("addressdetails", 1)
                            .queryParam("accept-language", "ko")
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(properties.timeout());
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "역지오코딩 호출에 실패했습니다.", e);
        }
    }

    private synchronized void throttle() {
        long wait = lastCallAt + MIN_INTERVAL_MILLIS - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "역지오코딩 호출이 중단되었습니다.");
            }
        }
        lastCallAt = System.currentTimeMillis();
    }

    static ReverseGeocodeResponse map(JsonNode address) {
        String dong = DONG_KEYS.stream()
                .map(k -> text(address, k))
                .filter(v -> v != null)
                .findFirst()
                .orElse(null);

        String road = text(address, "road");
        String roadAddress = null;
        if (road != null) {
            List<String> parts = new ArrayList<>();
            addIfPresent(parts, text(address, "province"));
            addIfPresent(parts, text(address, "city"));
            addIfPresent(parts, text(address, "borough"));
            addIfPresent(parts, text(address, "county"));
            parts.add(road);
            addIfPresent(parts, text(address, "house_number"));
            roadAddress = String.join(" ", parts);
        }
        return new ReverseGeocodeResponse(dong, roadAddress);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String s = value.asString().trim();
        return s.isEmpty() ? null : s;
    }

    private static void addIfPresent(List<String> parts, String value) {
        if (value != null && !parts.contains(value)) {
            parts.add(value);
        }
    }
}
