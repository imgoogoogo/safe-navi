package com.safenavi.backend.graphbuild;

import java.util.List;

/** A segment between two graph nodes, still keyed by OSM node ids. points run from osmNodeA to osmNodeB. */
public record RawSegment(long osmNodeA, long osmNodeB, List<double[]> points, double distanceMeters) {
}
