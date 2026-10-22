package com.safenavi.backend.graphbuild;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.collections.impl.map.mutable.primitive.LongDoubleHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;
import org.junit.jupiter.api.Test;

class WaySplitterTest {

    private final LongDoubleHashMap lats = new LongDoubleHashMap();
    private final LongDoubleHashMap lngs = new LongDoubleHashMap();

    private void coord(long id, double lat, double lng) {
        lats.put(id, lat);
        lngs.put(id, lng);
    }

    private static LongIntHashMap refCounts(List<long[]> ways) {
        LongIntHashMap counts = new LongIntHashMap();
        ways.forEach(w -> {
            for (long id : w) {
                counts.addToValue(id, 1);
            }
        });
        return counts;
    }

    @Test
    void splitsAtSharedNodesAndKeepsShapePoints() {
        // way A: 1-2-3-4-5, way B: 10-3-11 (crosses A at 3)
        for (long id = 1; id <= 5; id++) {
            coord(id, 37.5, 127.0 + id * 0.001);
        }
        coord(10, 37.49, 127.003);
        coord(11, 37.51, 127.003);
        List<long[]> ways = List.of(new long[] {1, 2, 3, 4, 5}, new long[] {10, 3, 11});

        List<RawSegment> segments = WaySplitter.split(ways, refCounts(ways), lats, lngs);

        assertThat(segments).extracting(s -> s.osmNodeA() + "-" + s.osmNodeB())
                .containsExactly("1-3", "3-5", "10-3", "3-11");
        assertThat(segments.get(0).points()).hasSize(3); // 1, 2 (shape point), 3
        assertThat(segments.get(0).distanceMeters()).isGreaterThan(150).isLessThan(200);
    }

    @Test
    void missingNodesCutTheWayIntoIndependentPieces() {
        coord(1, 37.5, 127.001);
        coord(2, 37.5, 127.002);
        // node 3 is outside the clip box
        coord(4, 37.5, 127.004);
        coord(5, 37.5, 127.005);
        List<long[]> ways = List.<long[]>of(new long[] {1, 2, 3, 4, 5});

        List<RawSegment> segments = WaySplitter.split(ways, refCounts(ways), lats, lngs);

        assertThat(segments).extracting(s -> s.osmNodeA() + "-" + s.osmNodeB()).containsExactly("1-2", "4-5");
    }

    @Test
    void closedLoopWithoutIntersectionsIsDropped() {
        coord(1, 37.5, 127.001);
        coord(2, 37.501, 127.001);
        coord(3, 37.501, 127.002);
        List<long[]> ways = List.<long[]>of(new long[] {1, 2, 3, 1});

        assertThat(WaySplitter.split(ways, refCounts(ways), lats, lngs)).isEmpty();
    }
}
