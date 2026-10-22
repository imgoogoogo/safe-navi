package com.safenavi.backend.common.geo;

import org.locationtech.jts.geom.Coordinate;

/**
 * Equirectangular projection to planar meters around a reference point. Accurate enough for
 * the few-kilometre areas a single route or safety query covers, and lets JTS work in meters.
 */
public final class LocalProjection {

    private final double refLat;
    private final double refLng;
    private final double metersPerDegreeLng;

    public LocalProjection(double refLat, double refLng) {
        this.refLat = refLat;
        this.refLng = refLng;
        this.metersPerDegreeLng = GeoUtils.METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(refLat));
    }

    public Coordinate toMeters(double lat, double lng) {
        return new Coordinate((lng - refLng) * metersPerDegreeLng, (lat - refLat) * GeoUtils.METERS_PER_DEGREE_LAT);
    }
}
