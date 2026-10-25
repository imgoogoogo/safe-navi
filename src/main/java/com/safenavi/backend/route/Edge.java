package com.safenavi.backend.route;

import com.safenavi.backend.graphbuild.RoadSegment;

/** One traversal direction of an undirected RoadSegment. */
public record Edge(RoadSegment segment, long from, long to) {
}
