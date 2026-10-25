package com.safenavi.backend.facility;

public record FacilityResponse(Long id, String type, double lat, double lng, boolean usedForRouting, String regionCode) {

    public static FacilityResponse from(Facility f) {
        return new FacilityResponse(f.getId(), f.getType(), f.getLat(), f.getLng(), f.getUsedForRouting(),
                f.getRegionCode());
    }
}
