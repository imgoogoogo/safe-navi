package com.safenavi.backend.route;

import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;

import com.safenavi.backend.common.geo.PolylineCodec;
import com.safenavi.backend.facility.FacilityType;
import com.safenavi.backend.graphbuild.RoadSegment;
import com.safenavi.backend.route.PresetWeightResolver.Preset;
import com.safenavi.backend.safety.FacilitySpatialIndex;
import com.safenavi.backend.safety.SafetyScoreCalculator;

/**
 * edge_cost = distanceMeters * (1 + alpha * danger_penalty), where
 * danger_penalty = clamp(1 - weighted safety score, 0.1, 0.9).
 * The per-type scores do not depend on the preset, so callers running several presets can compute
 * them once with {@link #typeScores} and reuse them with {@link #cost}.
 */
@Component
public class EdgeCostCalculator {

    public static final double MIN_DANGER_PENALTY = 0.1;
    public static final double MAX_DANGER_PENALTY = 0.9;

    public double calculate(RoadSegment segment, Preset preset, FacilitySpatialIndex facilitiesInArea) {
        return cost(segment, typeScores(segment, facilitiesInArea), preset);
    }

    public Map<FacilityType, Double> typeScores(RoadSegment segment, FacilitySpatialIndex facilitiesInArea) {
        Geometry line = toMeters(segment, facilitiesInArea);
        return SafetyScoreCalculator.typeScores(facilitiesInArea.countWithinInfluence(line), segment.getDistanceMeters());
    }

    public double cost(RoadSegment segment, Map<FacilityType, Double> typeScores, Preset preset) {
        double safety = SafetyScoreCalculator.weightedAverage(typeScores, preset.weights());
        double dangerPenalty = SafetyScoreCalculator.clamp(1 - safety, MIN_DANGER_PENALTY, MAX_DANGER_PENALTY);
        return segment.getDistanceMeters() * (1 + preset.alpha() * dangerPenalty);
    }

    private Geometry toMeters(RoadSegment segment, FacilitySpatialIndex index) {
        List<double[]> points = PolylineCodec.decode(segment.getGeometry());
        if (points.size() < 2) {
            points = List.of(new double[] {segment.getNodeA().getLat(), segment.getNodeA().getLng()},
                    new double[] {segment.getNodeB().getLat(), segment.getNodeB().getLng()});
        }
        Coordinate[] coords = new Coordinate[points.size()];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = index.getProjection().toMeters(points.get(i)[0], points.get(i)[1]);
        }
        return index.getGeometryFactory().createLineString(coords);
    }
}
