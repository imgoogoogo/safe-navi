package com.safenavi.backend.route;

import java.util.List;

/** polyline: [lat, lng] points from origin side to destination side. */
public record RouteDetail(String preset, double distanceMeters, int estimatedMinutes, double safetyScore,
        List<double[]> polyline) {
}
