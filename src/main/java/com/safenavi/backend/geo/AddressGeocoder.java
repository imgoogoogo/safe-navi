package com.safenavi.backend.geo;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.common.geo.Coordinate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Road-name address -> coordinate through the Kakao Local address search. Nominatim only resolves
 * Korean addresses to the street, not the building, so it is not usable for this direction.
 */
@Component
public class AddressGeocoder {

    private static final int MAX_ATTEMPTS = 5;
    /** Pacing for nationwide batches; a few thousand addresses still finish within minutes. */
    private static final long MIN_INTERVAL_MS = 100;
    /** Station names appended to the address, e.g. "난곡로 64 난우파출소". */
    private static final Pattern TRAILING_NAME = Pattern.compile("\\s+\\S*(파출소|지구대|치안센터)\\s*$");
    /** Missing space before the building number, e.g. "장월로174", "65번길2". */
    private static final Pattern GLUED_NUMBER = Pattern.compile("(로|길)(\\d+(-\\d+)?)(?=\\s|$)");

    private final KakaoLocalProperties properties;
    private final JsonMapper jsonMapper;
    private final WebClient webClient;
    private long lastCallAt;

    public AddressGeocoder(KakaoLocalProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.webClient = WebClient.builder().baseUrl(properties.baseUrl()).build();
    }

    public boolean isConfigured() {
        return properties.restApiKey() != null && !properties.restApiKey().isBlank();
    }

    /**
     * @return empty when the address is unknown to Kakao
     * @throws BusinessException EXTERNAL_API_ERROR when the call itself fails (bad key, quota, network)
     */
    public Optional<Coordinate> geocode(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        String normalized = address.replace("\"", " ").trim().replaceAll("\\s+", " ");
        Optional<Coordinate> found = lookup(normalized);
        if (found.isEmpty()) {
            // Source data often appends "(lot number)" or a station name that the geocoder rejects
            String cleaned = clean(normalized);
            if (!cleaned.equals(normalized)) {
                found = lookup(cleaned);
            }
        }
        return found;
    }

    static String clean(String address) {
        String cleaned = address.replaceAll("\\s*\\(.*", "");
        cleaned = TRAILING_NAME.matcher(cleaned).replaceAll("");
        cleaned = GLUED_NUMBER.matcher(cleaned).replaceAll("$1 $2");
        return cleaned.trim();
    }

    private Optional<Coordinate> lookup(String address) {
        if (address.isEmpty()) {
            return Optional.empty();
        }
        for (int attempt = 1;; attempt++) {
            try {
                return parse(jsonMapper.readTree(call(address)));
            } catch (RuntimeException e) {
                // 4xx (bad key, quota) arrives as BusinessException and is not retried; 5xx and network errors are
                if (e instanceof BusinessException || attempt == MAX_ATTEMPTS) {
                    throw e instanceof BusinessException be ? be : new BusinessException(
                            ErrorCode.EXTERNAL_API_ERROR, "Kakao 주소 변환 호출에 실패했습니다: " + e.getMessage());
                }
                pause(1000L * attempt);
            }
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Kakao 주소 변환이 중단되었습니다.");
        }
    }

    private synchronized String call(String address) {
        long wait = lastCallAt + MIN_INTERVAL_MS - System.currentTimeMillis();
        if (wait > 0) {
            pause(wait);
        }
        try {
            return request(address);
        } finally {
            lastCallAt = System.currentTimeMillis();
        }
    }

    private String request(String address) {
        return webClient.get()
                .uri(uri -> uri.path("/v2/local/search/address.json")
                        .queryParam("query", "{address}")
                        .queryParam("size", 1)
                        .build(address))
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                                "Kakao 주소 변환 오류: " + response.statusCode().value() + " " + body)))
                .bodyToMono(String.class)
                .block(properties.timeout());
    }

    static Optional<Coordinate> parse(JsonNode body) {
        JsonNode first = body == null ? null : body.path("documents").path(0);
        if (first == null || first.isMissingNode()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Coordinate(Double.parseDouble(first.path("y").asString("")),
                    Double.parseDouble(first.path("x").asString(""))));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
