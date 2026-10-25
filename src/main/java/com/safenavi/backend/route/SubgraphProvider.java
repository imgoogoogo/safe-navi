package com.safenavi.backend.route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.safenavi.backend.common.geo.BoundingBox;
import com.safenavi.backend.graphbuild.RoadNode;
import com.safenavi.backend.graphbuild.RoadSegment;
import com.safenavi.backend.graphbuild.RoadSegmentRepository;

@Component
public class SubgraphProvider {

    private final RoadSegmentRepository roadSegmentRepository;

    public SubgraphProvider(RoadSegmentRepository roadSegmentRepository) {
        this.roadSegmentRepository = roadSegmentRepository;
    }

    /**
     * Builds the undirected graph of segments touching the box. Nodes come from the segments'
     * fetched ends, so a segment crossing the box edge keeps its outside node too.
     */
    public Subgraph loadSubgraph(BoundingBox bbox) {
        List<RoadSegment> segments = roadSegmentRepository.findByBoundingBox(
                bbox.swLat(), bbox.swLng(), bbox.neLat(), bbox.neLng());
        return build(segments);
    }

    static Subgraph build(List<RoadSegment> segments) {
        Map<Long, RoadNode> nodes = new HashMap<>();
        Map<Long, List<Edge>> adjacency = new HashMap<>();
        for (RoadSegment s : segments) {
            long a = s.getNodeA().getId();
            long b = s.getNodeB().getId();
            nodes.putIfAbsent(a, s.getNodeA());
            nodes.putIfAbsent(b, s.getNodeB());
            adjacency.computeIfAbsent(a, k -> new ArrayList<>()).add(new Edge(s, a, b));
            adjacency.computeIfAbsent(b, k -> new ArrayList<>()).add(new Edge(s, b, a));
        }
        return new Subgraph(nodes, adjacency, segments);
    }
}
