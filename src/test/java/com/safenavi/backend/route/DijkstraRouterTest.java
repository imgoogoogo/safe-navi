package com.safenavi.backend.route;

import static com.safenavi.backend.route.RouteTestFixtures.node;
import static com.safenavi.backend.route.RouteTestFixtures.segment;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.safenavi.backend.graphbuild.RoadNode;

class DijkstraRouterTest {

    private final DijkstraRouter router = new DijkstraRouter();

    //   1 --- 2 --- 4        costs: 1-2: 1, 2-4: 1, 1-3: 1, 3-4: 5
    //    \         /
    //     ---3----
    private final RoadNode n1 = node(1, 0, 0);
    private final RoadNode n2 = node(2, 0, 1);
    private final RoadNode n3 = node(3, 1, 0);
    private final RoadNode n4 = node(4, 0, 2);
    private final Subgraph graph = SubgraphProvider.build(List.of(
            segment(12, n1, n2), segment(24, n2, n4), segment(13, n1, n3), segment(34, n3, n4)));
    private final Map<Long, Double> costs = Map.of(12L, 1.0, 24L, 1.0, 13L, 1.0, 34L, 5.0);

    @Test
    void findsCheapestPathOverUndirectedEdges() {
        var path = router.findShortestPath(graph.adjacency(), 4, 1, e -> costs.get(e.segment().getId())).orElseThrow();

        assertThat(path.nodeIds()).containsExactly(4L, 2L, 1L);
        assertThat(path.totalCost()).isEqualTo(2.0);
        assertThat(path.edges()).extracting(e -> e.segment().getId()).containsExactly(24L, 12L);
    }

    @Test
    void followsCostsNotHopCount() {
        Map<Long, Double> expensiveTop = Map.of(12L, 10.0, 24L, 10.0, 13L, 1.0, 34L, 1.0);
        var path = router.findShortestPath(graph.adjacency(), 1, 4, e -> expensiveTop.get(e.segment().getId())).orElseThrow();

        assertThat(path.nodeIds()).containsExactly(1L, 3L, 4L);
    }

    @Test
    void sameStartAndEndIsEmptyPath() {
        var path = router.findShortestPath(graph.adjacency(), 2, 2, e -> 1.0).orElseThrow();

        assertThat(path.edges()).isEmpty();
        assertThat(path.totalCost()).isZero();
    }

    @Test
    void unreachableReturnsEmpty() {
        RoadNode isolatedA = node(8, 5, 5);
        RoadNode isolatedB = node(9, 5, 6);
        Subgraph split = SubgraphProvider.build(List.of(segment(12, n1, n2), segment(89, isolatedA, isolatedB)));

        assertThat(router.findShortestPath(split.adjacency(), 1, 9, e -> 1.0)).isEmpty();
    }
}
