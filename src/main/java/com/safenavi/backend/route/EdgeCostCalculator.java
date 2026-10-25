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

/**
 * edge_cost = distance * (1 + 1 / (1 + weightedCount)), where weightedCount is the sum of
 * preset weight * facility count. No facility doubles the distance; each extra facility lowers it.
 * Counts do not depend on the preset, so {@link #facilityCounts} is computed once per segment
 * and reused across presets via {@link #cost}.
 */
@Component
public class EdgeCostCalculator {

    public double calculate(RoadSegment segment, Preset preset, FacilitySpatialIndex facilitiesInArea) {
        return cost(segment, facilityCounts(segment, facilitiesInArea), preset);
    }

    /** Per-type count of facilities within that type's influence radius of the segment line. */
    public Map<FacilityType, Integer> facilityCounts(RoadSegment segment, FacilitySpatialIndex facilitiesInArea) {
        return facilitiesInArea.countWithinInfluence(toMeters(segment, facilitiesInArea));
    }

    public double cost(RoadSegment segment, Map<FacilityType, Integer> counts, Preset preset) {
        double weightedCount = 0;
        for (Map.Entry<FacilityType, Double> w : preset.weights().entrySet()) {
            weightedCount += w.getValue() * counts.getOrDefault(w.getKey(), 0);
        }
        return segment.getDistanceMeters() * (1 + 1 / (1 + weightedCount));
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
