package com.safenavi.backend.route;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.safenavi.backend.common.geo.Coordinate;
import com.safenavi.backend.common.geo.GeoUtils;
import com.safenavi.backend.graphbuild.RoadNode;
import com.safenavi.backend.graphbuild.RoadSegment;

/** In-memory road graph of one bounding box: adjacency lists keyed by RoadNode id. */
public record Subgraph(Map<Long, RoadNode> nodes, Map<Long, List<Edge>> adjacency, List<RoadSegment> segments) {

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    /** Nearest node that has at least one edge. */
    public Optional<RoadNode> nearestNode(Coordinate point) {
        RoadNode best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Long id : adjacency.keySet()) {
            RoadNode node = nodes.get(id);
            double d = GeoUtils.haversineMeters(point.lat(), point.lng(), node.getLat(), node.getLng());
            if (d < bestDistance) {
                bestDistance = d;
                best = node;
            }
        }
        return Optional.ofNullable(best);
    }
}
