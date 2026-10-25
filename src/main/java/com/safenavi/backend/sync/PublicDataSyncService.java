package com.safenavi.backend.sync;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.common.geo.Coordinate;
import com.safenavi.backend.facility.FacilityRepository;
import com.safenavi.backend.facility.FacilityType;
import com.safenavi.backend.geo.AddressGeocoder;
import com.safenavi.backend.region.RegionCodeResolver;
import com.safenavi.backend.sync.PublicDataProperties.Source;
import com.safenavi.backend.sync.SyncResult.SourceResult;

/**
 * Pulls every enabled public-data source into Facility. Used by both the nightly
 * {@link PublicDataSyncScheduler} and the admin "sync now" endpoint; only one run at a time.
 */
@Service
public class PublicDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(PublicDataSyncService.class);

    /** Coordinates outside this box are data errors (0,0 placeholders, swapped lat/lng). */
    private static final double MIN_LAT = 33.0, MAX_LAT = 39.0, MIN_LNG = 124.0, MAX_LNG = 132.0;
    private static final int MAX_PAGES = 10_000;
    private static final int MAX_EXTERNAL_ID_LENGTH = 100;

    private final PublicDataProperties properties;
    private final PublicDataClient client;
    private final FacilityUpsertWriter writer;
    private final FacilityRepository facilityRepository;
    private final RegionCodeResolver regionCodeResolver;
    private final AddressGeocoder addressGeocoder;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public PublicDataSyncService(PublicDataProperties properties, PublicDataClient client, FacilityUpsertWriter writer,
            FacilityRepository facilityRepository, RegionCodeResolver regionCodeResolver,
            AddressGeocoder addressGeocoder) {
        this.properties = properties;
        this.client = client;
        this.writer = writer;
        this.facilityRepository = facilityRepository;
        this.regionCodeResolver = regionCodeResolver;
        this.addressGeocoder = addressGeocoder;
    }

    public boolean isRunning() {
        return running.get();
    }

    /** @throws BusinessException SYNC_IN_PROGRESS if another run has not finished */
    public SyncResult syncAll() {
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException(ErrorCode.SYNC_IN_PROGRESS);
        }
        try {
            LocalDateTime startedAt = LocalDateTime.now();
            if (properties.serviceKey() == null || properties.serviceKey().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "PUBLIC_DATA_SERVICE_KEY 환경변수가 설정되지 않았습니다.");
            }
            regionCodeResolver.refresh();
            List<SourceResult> results = new ArrayList<>();
            for (Source source : properties.sources()) {
                if (source.enabled()) {
                    results.add(syncSource(source));
                }
            }
            return new SyncResult(startedAt, LocalDateTime.now(), results);
        } finally {
            running.set(false);
        }
    }

    private SourceResult syncSource(Source source) {
        String configError = validate(source);
        if (configError != null) {
            log.warn("Skipping public data source {}: {}", source.name(), configError);
            return new SourceResult(source.name(), source.facilityType(), 0, 0, 0, 0, configError);
        }
        int fetched = 0;
        int skipped = 0;
        Map<String, FacilityRecord> records = new LinkedHashMap<>();
        try {
            for (int page = 1; page <= MAX_PAGES; page++) {
                PublicDataPage result = client.fetchPage(source, page);
                for (Map<String, String> item : result.items()) {
                    fetched++;
                    FacilityRecord record = toRecord(source, item);
                    if (record == null) {
                        skipped++;
                    } else {
                        records.put(record.externalId(), record);
                    }
                }
                if (result.items().isEmpty() || fetched >= result.totalCount()) {
                    break;
                }
            }
            // Only a complete fetch may retire rows that disappeared from the source
            Set<String> stale = new HashSet<>(facilityRepository.findExternalIdsBySource(source.name()));
            stale.removeAll(records.keySet());
            writer.write(records.values(), source.name(), stale);
            log.info("Public data sync {}: fetched={}, upserted={}, skipped={}, deleted={}",
                    source.name(), fetched, records.size(), skipped, stale.size());
            return new SourceResult(source.name(), source.facilityType(), fetched, records.size(), skipped,
                    stale.size(), null);
        } catch (RuntimeException e) {
            log.error("Public data sync {} failed after {} items", source.name(), fetched, e);
            return new SourceResult(source.name(), source.facilityType(), fetched, 0, skipped, 0, e.getMessage());
        }
    }

    private String validate(Source source) {
        if (source.name() == null || source.name().isBlank() || source.name().length() > 30) {
            return "name must be 1-30 characters";
        }
        if (FacilityType.fromCode(source.facilityType()).isEmpty()) {
            return "unknown facility-type " + source.facilityType();
        }
        if (source.url() == null || source.url().isBlank()) {
            return "url is not configured";
        }
        if (source.addressField() != null) {
            return addressGeocoder.isConfigured() ? null : "address-field needs KAKAO_REST_API_KEY for geocoding";
        }
        if (source.latField() == null || source.lngField() == null) {
            return "lat-field and lng-field, or address-field, are required";
        }
        return null;
    }

    FacilityRecord toRecord(Source source, Map<String, String> item) {
        Double lat;
        Double lng;
        if (source.addressField() != null) {
            Optional<Coordinate> point = addressGeocoder.geocode(item.get(source.addressField()));
            if (point.isEmpty()) {
                log.warn("Public data sync {}: no coordinate for address '{}'", source.name(),
                        item.get(source.addressField()));
                return null;
            }
            lat = point.get().lat();
            lng = point.get().lng();
        } else {
            lat = parse(item.get(source.latField()));
            lng = parse(item.get(source.lngField()));
        }
        if (lat == null || lng == null) {
            return null;
        }
        if (lat < MIN_LAT || lat > MAX_LAT || lng < MIN_LNG || lng > MAX_LNG) {
            return null;
        }
        return new FacilityRecord(externalId(source, item, lat, lng), source.facilityType(), lat, lng, source.name(),
                regionCodeResolver.resolve(lat, lng));
    }

    private static String externalId(Source source, Map<String, String> item, double lat, double lng) {
        String rawId = source.idField() == null ? null : item.get(source.idField());
        String id = rawId != null && !rawId.isBlank()
                ? source.name() + ":" + rawId.trim()
                : String.format(Locale.ROOT, "%s:%.6f,%.6f", source.name(), lat, lng);
        return id.length() <= MAX_EXTERNAL_ID_LENGTH ? id
                : source.name() + ":" + UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));
    }

    private static Double parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            double d = Double.parseDouble(value.trim());
            return Double.isFinite(d) ? d : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
