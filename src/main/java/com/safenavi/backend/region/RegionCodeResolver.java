package com.safenavi.backend.region;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.safenavi.backend.graphbuild.RoadNodeRepository;

/**
 * Maps a coordinate to a 시군구 regionCode using the extents of regions that already have a road
 * graph (each graph build is tagged with its regionCode). Extents of neighbouring regions overlap,
 * so a point inside several of them goes to the one whose centre is closest: an approximation that
 * is good enough for partitioning and alert topics, not an administrative-boundary lookup.
 */
@Component
public class RegionCodeResolver {

    public static final String UNKNOWN = "UNKNOWN";

    private final RoadNodeRepository roadNodeRepository;
    private volatile List<RegionExtent> extents;

    public RegionCodeResolver(RoadNodeRepository roadNodeRepository) {
        this.roadNodeRepository = roadNodeRepository;
    }

    public String resolve(double lat, double lng) {
        return extents().stream()
                .filter(e -> e.contains(lat, lng))
                .min(Comparator.comparingDouble(e -> e.centerDistanceSq(lat, lng)))
                .map(RegionExtent::regionCode)
                .orElse(UNKNOWN);
    }

    /** Call after the road graph of any region changes. */
    public void refresh() {
        extents = List.copyOf(roadNodeRepository.findRegionExtents());
    }

    private List<RegionExtent> extents() {
        List<RegionExtent> current = extents;
        if (current == null) {
            refresh();
            current = extents;
        }
        return current;
    }
}
