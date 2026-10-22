package com.safenavi.backend.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bulk upsert keyed on Facility.externalId (uk_facility_externalid) with MySQL
 * INSERT ... ON DUPLICATE KEY UPDATE. usedForRouting is only set on insert, so an admin's
 * manual exclusion survives later syncs.
 */
@Component
public class FacilityUpsertWriter {

    private static final int BATCH_SIZE = 1000;

    private static final String UPSERT_SQL = """
            INSERT INTO Facility (type, lat, lng, usedForRouting, externalId, sourceDataType, regionCode)
            VALUES (?, ?, ?, TRUE, ?, ?, ?) AS incoming
            ON DUPLICATE KEY UPDATE type = incoming.type, lat = incoming.lat, lng = incoming.lng,
                sourceDataType = incoming.sourceDataType, regionCode = incoming.regionCode
            """;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public FacilityUpsertWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /** Upserts all records and deletes this source's rows whose externalId is in staleIds, atomically. */
    @Transactional
    public void write(Collection<FacilityRecord> records, String sourceDataType, Collection<String> staleIds) {
        List<FacilityRecord> list = new ArrayList<>(records);
        for (int from = 0; from < list.size(); from += BATCH_SIZE) {
            List<FacilityRecord> chunk = list.subList(from, Math.min(list.size(), from + BATCH_SIZE));
            jdbcTemplate.batchUpdate(UPSERT_SQL, chunk, chunk.size(), (ps, r) -> {
                ps.setString(1, r.type());
                ps.setDouble(2, r.lat());
                ps.setDouble(3, r.lng());
                ps.setString(4, r.externalId());
                ps.setString(5, r.sourceDataType());
                ps.setString(6, r.regionCode());
            });
        }
        List<String> stale = new ArrayList<>(staleIds);
        for (int from = 0; from < stale.size(); from += BATCH_SIZE) {
            namedJdbcTemplate.update(
                    "DELETE FROM Facility WHERE sourceDataType = :source AND externalId IN (:ids)",
                    Map.of("source", sourceDataType, "ids", stale.subList(from, Math.min(stale.size(), from + BATCH_SIZE))));
        }
    }
}
