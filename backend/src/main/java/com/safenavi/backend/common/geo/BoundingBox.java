package com.safenavi.backend.common.geo;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;

public record BoundingBox(double swLat, double swLng, double neLat, double neLng) {

    /** Largest box the public list endpoints accept (roughly 55km x 45km at Korean latitudes). */
    public static final double MAX_SPAN_DEGREES = 0.5;

    public BoundingBox {
        new Coordinate(swLat, swLng);
        new Coordinate(neLat, neLng);
        if (swLat > neLat || swLng > neLng) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "남서(sw) 좌표가 북동(ne) 좌표보다 작아야 합니다.");
        }
    }

    public BoundingBox requireMaxSpan(double maxDegrees) {
        if (neLat - swLat > maxDegrees || neLng - swLng > maxDegrees) {
            throw new BusinessException(ErrorCode.BBOX_TOO_LARGE);
        }
        return this;
    }

    public boolean contains(double lat, double lng) {
        return lat >= swLat && lat <= neLat && lng >= swLng && lng <= neLng;
    }

    public Coordinate center() {
        return new Coordinate((swLat + neLat) / 2, (swLng + neLng) / 2);
    }
}
