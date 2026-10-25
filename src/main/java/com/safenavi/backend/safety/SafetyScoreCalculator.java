package com.safenavi.backend.safety;

import java.util.EnumMap;
import java.util.Map;

import com.safenavi.backend.facility.FacilityType;

/**
 * Radius-density safety model shared by routing (per road segment) and the point safety score.
 *
 * A facility type's score is its observed count divided by the count that saturates the score,
 * clamped to [0, 1]. The saturating count is defined for a 100m corridor of the type's radius
 * ({@link FacilityType#getSaturationPer100m()}) and scaled by area, so a 20m segment and a
 * 400m segment, or a point query with any radius, are judged by the same density.
 */
public final class SafetyScoreCalculator {

    private static final double REFERENCE_LENGTH_METERS = 100.0;

    private SafetyScoreCalculator() {
    }

    /** Density score of one type for a corridor (lengthMeters > 0) or a circle (lengthMeters = 0). */
    public static double typeScore(FacilityType type, int count, double lengthMeters, double radiusMeters) {
        double area = corridorArea(lengthMeters, radiusMeters);
        double referenceArea = corridorArea(REFERENCE_LENGTH_METERS, radiusMeters);
        double expected = type.getSaturationPer100m() * area / referenceArea;
        if (expected <= 0) {
            return 0.0;
        }
        return clamp(count / expected, 0.0, 1.0);
    }

    /** Scores for all types, each counted within its own influence radius. */
    public static Map<FacilityType, Double> typeScores(Map<FacilityType, Integer> counts, double lengthMeters) {
        Map<FacilityType, Double> scores = new EnumMap<>(FacilityType.class);
        for (FacilityType type : FacilityType.values()) {
            scores.put(type, typeScore(type, counts.getOrDefault(type, 0), lengthMeters, type.getInfluenceRadiusMeters()));
        }
        return scores;
    }

    /** Weighted average of type scores, in [0, 1]. Types without a weight are ignored. */
    public static double weightedAverage(Map<FacilityType, Double> typeScores, Map<FacilityType, Double> weights) {
        double weightSum = 0;
        double sum = 0;
        for (Map.Entry<FacilityType, Double> w : weights.entrySet()) {
            weightSum += w.getValue();
            sum += w.getValue() * typeScores.getOrDefault(w.getKey(), 0.0);
        }
        return weightSum == 0 ? 0.0 : sum / weightSum;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double corridorArea(double lengthMeters, double radiusMeters) {
        return 2 * radiusMeters * lengthMeters + Math.PI * radiusMeters * radiusMeters;
    }
}
