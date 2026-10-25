package com.safenavi.backend.graphbuild;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class WalkableWayFilterTest {

    @Test
    void acceptsPedestrianAndResidentialRoads() {
        assertThat(WalkableWayFilter.isWalkable(Map.of("highway", "footway"))).isTrue();
        assertThat(WalkableWayFilter.isWalkable(Map.of("highway", "residential"))).isTrue();
    }

    @Test
    void rejectsMotorwaysUnlessFootIsAllowed() {
        assertThat(WalkableWayFilter.isWalkable(Map.of("highway", "motorway"))).isFalse();
        assertThat(WalkableWayFilter.isWalkable(Map.of("highway", "trunk", "foot", "yes"))).isTrue();
    }

    @Test
    void rejectsPrivateAccessAndAreasAndNonHighways() {
        assertThat(WalkableWayFilter.isWalkable(Map.of("highway", "service", "access", "private"))).isFalse();
        assertThat(WalkableWayFilter.isWalkable(Map.of("highway", "footway", "foot", "no"))).isFalse();
        assertThat(WalkableWayFilter.isWalkable(Map.of("highway", "pedestrian", "area", "yes"))).isFalse();
        assertThat(WalkableWayFilter.isWalkable(Map.of("building", "yes"))).isFalse();
    }
}
