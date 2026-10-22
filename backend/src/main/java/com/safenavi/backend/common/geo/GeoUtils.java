package com.safenavi.backend.common.geo;

public final class GeoUtils {

    public static final double EARTH_RADIUS_METERS = 6_371_008.8;
    public static final double METERS_PER_DEGREE_LAT = 111_320.0;

    private GeoUtils() {
    }

    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    public static double haversineMeters(Coordinate a, Coordinate b) {
        return haversineMeters(a.lat(), a.lng(), b.lat(), b.lng());
    }

    /** Box of +-radius around a point, for coarse DB filtering before an exact distance check. */
    public static BoundingBox boxAround(double lat, double lng, double radiusMeters) {
        double dLat = radiusMeters / METERS_PER_DEGREE_LAT;
        double dLng = radiusMeters / (METERS_PER_DEGREE_LAT * Math.max(0.01, Math.cos(Math.toRadians(lat))));
        return new BoundingBox(Math.max(-90, lat - dLat), Math.max(-180, lng - dLng),
                Math.min(90, lat + dLat), Math.min(180, lng + dLng));
    }
}
