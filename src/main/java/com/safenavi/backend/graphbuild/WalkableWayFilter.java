package com.safenavi.backend.graphbuild;

import java.util.Map;
import java.util.Set;

/** Decides from OSM tags whether a way is usable by pedestrians. */
public final class WalkableWayFilter {

    private static final Set<String> WALKABLE_HIGHWAYS = Set.of(
            "footway", "pedestrian", "path", "steps", "living_street", "residential", "service",
            "unclassified", "track", "cycleway", "corridor", "crossing",
            "tertiary", "tertiary_link", "secondary", "secondary_link", "primary", "primary_link", "road");
    /** Walkable only when explicitly tagged foot=yes|designated|permissive. */
    private static final Set<String> FOOT_TAG_REQUIRED = Set.of("trunk", "trunk_link", "motorway", "motorway_link");
    private static final Set<String> FOOT_ALLOWED = Set.of("yes", "designated", "permissive");
    private static final Set<String> NO_ACCESS = Set.of("no", "private");

    private WalkableWayFilter() {
    }

    public static boolean isWalkable(Map<String, String> tags) {
        String highway = tags.get("highway");
        // area=yes marks pedestrian plazas drawn as polygons, not walkable lines
        if (highway == null || "yes".equals(tags.get("area"))) {
            return false;
        }
        return isWalkableHighway(highway, tags);
    }

    private static boolean isWalkableHighway(String highway, Map<String, String> tags) {
        String foot = tags.get("foot");
        if (foot != null && NO_ACCESS.contains(foot)) {
            return false;
        }
        if (foot != null && FOOT_ALLOWED.contains(foot)) {
            return WALKABLE_HIGHWAYS.contains(highway) || FOOT_TAG_REQUIRED.contains(highway);
        }
        String access = tags.get("access");
        if (access != null && NO_ACCESS.contains(access)) {
            return false;
        }
        return WALKABLE_HIGHWAYS.contains(highway);
    }
}
