package com.safenavi.backend.region;

public record RegionExtent(String regionCode, double swLat, double swLng, double neLat, double neLng) {

    public boolean contains(double lat, double lng) {
        return lat >= swLat && lat <= neLat && lng >= swLng && lng <= neLng;
    }

    public double centerDistanceSq(double lat, double lng) {
        double dLat = lat - (swLat + neLat) / 2;
        double dLng = lng - (swLng + neLng) / 2;
        return dLat * dLat + dLng * dLng;
    }
}
