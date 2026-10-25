package com.safenavi.backend.sync;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * public-data.* in application.yml. Each source maps one data.go.kr style API to a Facility type;
 * field names are configurable because each dataset names its id/coordinate columns differently.
 */
@ConfigurationProperties(prefix = "public-data")
public record PublicDataProperties(String serviceKey, String cron, int pageSize, Duration timeout,
        List<Source> sources) {

    public PublicDataProperties {
        sources = sources == null ? List.of() : List.copyOf(sources);
        if (pageSize <= 0) {
            pageSize = 1000;
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(30);
        }
    }

    /**
     * @param name        stored as Facility.sourceDataType (max 30 chars) and used to prefix externalId
     * @param format       "json", "xml", or "odcloud" (api.odcloud.kr: page/perPage, rows under "data")
     * @param idField      optional; when absent, the externalId is derived from the coordinates
     * @param addressField for sources without coordinates: the row's address is geocoded instead
     */
    public record Source(String name, String facilityType, String format, String url, String idField,
            String latField, String lngField, String addressField, boolean enabled) {

        public boolean isXml() {
            return "xml".equalsIgnoreCase(format);
        }

        public boolean isOdcloud() {
            return "odcloud".equalsIgnoreCase(format);
        }
    }
}
