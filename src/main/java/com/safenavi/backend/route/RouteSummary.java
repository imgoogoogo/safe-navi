package com.safenavi.backend.route;

/** safetyScore is 0-100, measured with the default preset's weights so presets can be compared. */
public record RouteSummary(String preset, double distanceMeters, int estimatedMinutes, double safetyScore) {
}
