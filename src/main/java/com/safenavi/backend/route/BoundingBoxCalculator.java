package com.safenavi.backend.route;

import org.springframework.stereotype.Component;

import com.safenavi.backend.common.geo.BoundingBox;
import com.safenavi.backend.common.geo.Coordinate;
import com.safenavi.backend.common.geo.GeoUtils;

@Component
public class BoundingBoxCalculator {

    public static final double DEFAULT_MARGIN_METERS = 500;

    public BoundingBox calculate(Coordinate origin, Coordinate dest) {
        return calculate(origin, dest, DEFAULT_MARGIN_METERS);
    }

    /** Box enclosing both points, grown by marginMeters on every side. */
    public BoundingBox calculate(Coordinate origin, Coordinate dest, double marginMeters) {
        double minLat = Math.min(origin.lat(), dest.lat());
        double maxLat = Math.max(origin.lat(), dest.lat());
        double minLng = Math.min(origin.lng(), dest.lng());
        double maxLng = Math.max(origin.lng(), dest.lng());
        return expand(new BoundingBox(minLat, minLng, maxLat, maxLng), marginMeters);
    }

    public BoundingBox expand(BoundingBox box, double marginMeters) {
        double dLat = marginMeters / GeoUtils.METERS_PER_DEGREE_LAT;
        // Use the latitude closest to a pole so the margin is never smaller than requested
        double maxAbsLat = Math.max(Math.abs(box.swLat()), Math.abs(box.neLat()));
        double dLng = marginMeters / (GeoUtils.METERS_PER_DEGREE_LAT * Math.max(0.01, Math.cos(Math.toRadians(maxAbsLat))));
        return new BoundingBox(Math.max(-90, box.swLat() - dLat), Math.max(-180, box.swLng() - dLng),
                Math.min(90, box.neLat() + dLat), Math.min(180, box.neLng() + dLng));
    }
}
