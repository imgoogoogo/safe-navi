package com.safenavi.backend.sync;

public record FacilityRecord(String externalId, String type, double lat, double lng, String sourceDataType,
        String regionCode) {
}
