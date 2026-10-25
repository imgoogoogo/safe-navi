package com.safenavi.backend.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import org.springframework.stereotype.Component;

/** Plain Dijkstra over an adjacency map, with lazy deletion of stale queue entries. */
@Component
public class DijkstraRouter {

    public record PathResult(double totalCost, List<Long> nodeIds, List<Edge> edges) {
    }

    private record QueueEntry(long nodeId, double cost) {
    }

    public Optional<PathResult> findShortestPath(Map<Long, List<Edge>> graph, long startNodeId, long endNodeId,
            ToDoubleFunction<Edge> edgeCost) {
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Edge> prev = new HashMap<>();
        Set<Long> settled = new HashSet<>();
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>((a, b) -> Double.compare(a.cost(), b.cost()));

        dist.put(startNodeId, 0.0);
        queue.add(new QueueEntry(startNodeId, 0.0));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            if (!settled.add(current.nodeId())) {
                continue;
            }
            if (current.nodeId() == endNodeId) {
                return Optional.of(backtrack(prev, startNodeId, endNodeId, current.cost()));
            }
            for (Edge edge : graph.getOrDefault(current.nodeId(), List.of())) {
                if (settled.contains(edge.to())) {
                    continue;
                }
                double cost = edgeCost.applyAsDouble(edge);
                if (cost < 0) {
                    throw new IllegalArgumentException("Negative edge cost on segment " + edge.segment().getId());
                }
                double candidate = current.cost() + cost;
                if (candidate < dist.getOrDefault(edge.to(), Double.POSITIVE_INFINITY)) {
                    dist.put(edge.to(), candidate);
                    prev.put(edge.to(), edge);
                    queue.add(new QueueEntry(edge.to(), candidate));
                }
            }
        }
        return Optional.empty();
    }

    private PathResult backtrack(Map<Long, Edge> prev, long start, long end, double totalCost) {
        List<Edge> edges = new ArrayList<>();
        List<Long> nodeIds = new ArrayList<>();
        long node = end;
        nodeIds.add(node);
        while (node != start) {
            Edge edge = prev.get(node);
            edges.add(edge);
            node = edge.from();
            nodeIds.add(node);
        }
        Collections.reverse(edges);
        Collections.reverse(nodeIds);
        return new PathResult(totalCost, nodeIds, edges);
    }
}
