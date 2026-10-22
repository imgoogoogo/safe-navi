package com.safenavi.backend.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

import com.safenavi.backend.common.geo.BoundingBox;
import com.safenavi.backend.common.geo.Coordinate;
import com.safenavi.backend.common.geo.GeoUtils;

class BoundingBoxCalculatorTest {

    private final BoundingBoxCalculator calculator = new BoundingBoxCalculator();

    @Test
    void enclosesBothPointsWithMarginOnEverySide() {
        Coordinate a = new Coordinate(37.51, 127.03);
        Coordinate b = new Coordinate(37.49, 127.05);
        BoundingBox box = calculator.calculate(a, b, 500);

        assertThat(box.contains(a.lat(), a.lng())).isTrue();
        assertThat(box.contains(b.lat(), b.lng())).isTrue();
        assertThat(GeoUtils.haversineMeters(box.swLat(), 127.04, 37.49, 127.04)).isCloseTo(500, within(5.0));
        assertThat(GeoUtils.haversineMeters(37.51, box.neLng(), 37.51, 127.05)).isGreaterThanOrEqualTo(499.0);
    }
}
