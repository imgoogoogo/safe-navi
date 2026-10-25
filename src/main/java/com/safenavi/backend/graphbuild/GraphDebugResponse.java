package com.safenavi.backend.graphbuild;

import java.util.List;

/**
 * Road graph of a bounding box for the map-test page. degree = number of segments meeting at the node
 * (1: dead end or clip edge, 3+: intersection).
 */
public record GraphDebugResponse(List<NodeView> nodes, List<SegmentView> segments) {

    public record NodeView(Long id, double lat, double lng, int degree) {
    }

    public record SegmentView(Long id, Long nodeAId, Long nodeBId, double distanceMeters, List<double[]> geometry) {
    }
}
