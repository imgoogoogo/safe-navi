package com.safenavi.backend.route;

import static com.safenavi.backend.route.RouteTestFixtures.node;
import static com.safenavi.backend.route.RouteTestFixtures.segment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;

import com.safenavi.backend.common.geo.LocalProjection;
import com.safenavi.backend.facility.Facility;
import com.safenavi.backend.graphbuild.RoadSegment;
import com.safenavi.backend.safety.FacilitySpatialIndex;

class EdgeCostCalculatorTest {

    private final EdgeCostCalculator calculator = new EdgeCostCalculator();
    private final PresetWeightResolver presets = new PresetWeightResolver();
    // ~111m segment running north from (37.5, 127.0)
    private final RoadSegment segment = segment(1, node(1, 37.5, 127.0), node(2, 37.501, 127.0));

    private FacilitySpatialIndex index(List<Facility> facilities) {
        return new FacilitySpatialIndex(facilities, new LocalProjection(37.5, 127.0), new GeometryFactory());
    }

    private static List<Facility> cctvs(int n) {
        List<Facility> cctvs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            cctvs.add(new Facility("cctv", 37.5 + i * 0.0002, 127.0001, "c" + i, "test", "11680"));
        }
        return cctvs;
    }

    @Test
    void noFacilitiesGivesFullPenalty() {
        double cost = calculator.calculate(segment, presets.resolve("cctv"), index(List.of()));

        assertThat(cost).isCloseTo(segment.getDistanceMeters() * 2.0, within(1e-6));
    }

    @Test
    void penaltyFollowsRawCount() {
        double cost = calculator.calculate(segment, presets.resolve("cctv"), index(cctvs(2)));

        // weighted count 2 -> surcharge 1/3
        assertThat(cost).isCloseTo(segment.getDistanceMeters() * (1 + 1.0 / 3), within(1e-6));
    }

    @Test
    void everyExtraFacilityLowersCost() {
        double previous = Double.MAX_VALUE;
        for (int n = 0; n <= 5; n++) {
            double cost = calculator.calculate(segment, presets.resolve("cctv"), index(cctvs(n)));
            assertThat(cost).isLessThan(previous);
            previous = cost;
        }
    }

    @Test
    void facilitiesOutsideRadiusAreIgnored() {
        // ~900m east: beyond the 50m CCTV radius
        List<Facility> far = List.of(new Facility("cctv", 37.5005, 127.01, "far", "test", "11680"));
        double cost = calculator.calculate(segment, presets.resolve("cctv"), index(far));

        assertThat(cost).isCloseTo(segment.getDistanceMeters() * 2.0, within(1e-6));
    }

    @Test
    void presetOnlyWeighsItsOwnFacilityTypes() {
        List<Facility> lights = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            lights.add(new Facility("streetlight", 37.5 + i * 0.0001, 127.0, "l" + i, "test", "11680"));
        }
        FacilitySpatialIndex idx = index(lights);

        assertThat(calculator.calculate(segment, presets.resolve("streetlight"), idx))
                .isLessThan(calculator.calculate(segment, presets.resolve("cctv"), idx));
    }
}
