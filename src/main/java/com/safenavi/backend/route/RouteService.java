package com.safenavi.backend.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.common.geo.BoundingBox;
import com.safenavi.backend.common.geo.Coordinate;
import com.safenavi.backend.common.geo.GeoUtils;
import com.safenavi.backend.common.geo.LocalProjection;
import com.safenavi.backend.common.geo.PolylineCodec;
import com.safenavi.backend.facility.Facility;
import com.safenavi.backend.facility.FacilityRepository;
import com.safenavi.backend.facility.FacilityType;
import com.safenavi.backend.graphbuild.RoadNode;
import com.safenavi.backend.graphbuild.RoadSegment;
import com.safenavi.backend.route.DijkstraRouter.PathResult;
import com.safenavi.backend.route.PresetWeightResolver.Preset;
import com.safenavi.backend.safety.FacilitySpatialIndex;
import com.safenavi.backend.safety.SafetyScoreCalculator;

@Service
public class RouteService {

    /** Walking routes only: refuse pairs further apart than this (straight line). */
    static final double MAX_ROUTE_DISTANCE_METERS = 20_000;
    /** Origin/destination must be this close to the road graph. */
    static final double MAX_SNAP_DISTANCE_METERS = 1_000;
    static final double WALKING_METERS_PER_MINUTE = 75; // 4.5 km/h

    private final BoundingBoxCalculator boundingBoxCalculator;
    private final SubgraphProvider subgraphProvider;
    private final FacilityRepository facilityRepository;
    private final PresetWeightResolver presetWeightResolver;
    private final EdgeCostCalculator edgeCostCalculator;
    private final DijkstraRouter dijkstraRouter;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public RouteService(BoundingBoxCalculator boundingBoxCalculator, SubgraphProvider subgraphProvider,
            FacilityRepository facilityRepository, PresetWeightResolver presetWeightResolver,
            EdgeCostCalculator edgeCostCalculator, DijkstraRouter dijkstraRouter) {
        this.boundingBoxCalculator = boundingBoxCalculator;
        this.subgraphProvider = subgraphProvider;
        this.facilityRepository = facilityRepository;
        this.presetWeightResolver = presetWeightResolver;
        this.edgeCostCalculator = edgeCostCalculator;
        this.dijkstraRouter = dijkstraRouter;
    }

    @Transactional(readOnly = true)
    public List<RouteSummary> compareRoutes(Coordinate origin, Coordinate dest) {
        RoutingContext ctx = prepare(origin, dest);
        List<RouteSummary> summaries = new ArrayList<>();
        for (Preset preset : presetWeightResolver.all()) {
            PathResult path = ctx.route(preset);
            summaries.add(new RouteSummary(preset.name(), ctx.distance(path), estimatedMinutes(ctx.distance(path)),
                    ctx.safetyScore(path)));
        }
        return summaries;
    }

    @Transactional(readOnly = true)
    public RouteDetail findRoute(Coordinate origin, Coordinate dest, String presetName) {
        Preset preset = presetWeightResolver.resolve(presetName == null || presetName.isBlank()
                ? PresetWeightResolver.DEFAULT : presetName);
        RoutingContext ctx = prepare(origin, dest);
        PathResult path = ctx.route(preset);
        double distance = ctx.distance(path);
        return new RouteDetail(preset.name(), distance, estimatedMinutes(distance), ctx.safetyScore(path),
                buildPolyline(path, ctx.startNode()));
    }

    private RoutingContext prepare(Coordinate origin, Coordinate dest) {
        if (GeoUtils.haversineMeters(origin, dest) > MAX_ROUTE_DISTANCE_METERS) {
            throw new BusinessException(ErrorCode.ROUTE_TOO_FAR);
        }
        BoundingBox bbox = boundingBoxCalculator.calculate(origin, dest);
        Subgraph graph = subgraphProvider.loadSubgraph(bbox);
        if (graph.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_ROAD_DATA);
        }
        RoadNode start = snap(graph, origin);
        RoadNode end = snap(graph, dest);

        // Single facility query for all presets; widened so segments near the box edge see nearby facilities
        BoundingBox facilityBox = boundingBoxCalculator.expand(bbox, FacilityType.maxInfluenceRadius());
        List<Facility> facilities = facilityRepository.findByBoundingBox(facilityBox.swLat(), facilityBox.swLng(),
                        facilityBox.neLat(), facilityBox.neLng())
                .stream().filter(Facility::getUsedForRouting).toList();
        Coordinate center = bbox.center();
        FacilitySpatialIndex index = new FacilitySpatialIndex(facilities,
                new LocalProjection(center.lat(), center.lng()), geometryFactory);
        return new RoutingContext(graph, start, end, index);
    }

    private RoadNode snap(Subgraph graph, Coordinate point) {
        RoadNode node = graph.nearestNode(point)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_ROAD_DATA));
        if (GeoUtils.haversineMeters(point.lat(), point.lng(), node.getLat(), node.getLng()) > MAX_SNAP_DISTANCE_METERS) {
            throw new BusinessException(ErrorCode.NO_ROAD_DATA, "출발지/도착지 근처에 도로 데이터가 없습니다.");
        }
        return node;
    }

    private static int estimatedMinutes(double distanceMeters) {
        return (int) Math.ceil(distanceMeters / WALKING_METERS_PER_MINUTE);
    }

    /** Joins segment geometries in travel order, reversing segments walked from nodeB to nodeA. */
    private static List<double[]> buildPolyline(PathResult path, RoadNode start) {
        List<double[]> polyline = new ArrayList<>();
        if (path.edges().isEmpty()) {
            polyline.add(new double[] {start.getLat(), start.getLng()});
            return polyline;
        }
        for (Edge edge : path.edges()) {
            List<double[]> points = PolylineCodec.decode(edge.segment().getGeometry());
            if (edge.from() != edge.segment().getNodeA().getId()) {
                Collections.reverse(points);
            }
            for (double[] p : points) {
                double[] last = polyline.isEmpty() ? null : polyline.get(polyline.size() - 1);
                if (last == null || last[0] != p[0] || last[1] != p[1]) {
                    polyline.add(p);
                }
            }
        }
        return polyline;
    }

    /** Graph, snapped endpoints and facility index of one request, with per-segment facility counts cached across presets. */
    private final class RoutingContext {

        private final Subgraph graph;
        private final RoadNode start;
        private final RoadNode end;
        private final FacilitySpatialIndex index;
        private final Map<Long, Map<FacilityType, Integer>> countCache = new HashMap<>();
        private final Map<FacilityType, Double> scoreWeights = presetWeightResolver.resolve(PresetWeightResolver.DEFAULT).weights();

        RoutingContext(Subgraph graph, RoadNode start, RoadNode end, FacilitySpatialIndex index) {
            this.graph = graph;
            this.start = start;
            this.end = end;
            this.index = index;
        }

        RoadNode startNode() {
            return start;
        }

        PathResult route(Preset preset) {
            Map<Long, Double> costs = new HashMap<>();
            return dijkstraRouter.findShortestPath(graph.adjacency(), start.getId(), end.getId(),
                            edge -> costs.computeIfAbsent(edge.segment().getId(),
                                    id -> edgeCostCalculator.cost(edge.segment(), counts(edge.segment()), preset)))
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_NOT_FOUND));
        }

        double distance(PathResult path) {
            return Math.round(path.edges().stream().mapToDouble(e -> e.segment().getDistanceMeters()).sum() * 10) / 10.0;
        }

        /** Length-weighted default-preset density safety along the path, 0-100 (not the count-based routing cost). */
        double safetyScore(PathResult path) {
            double length = 0;
            double weighted = 0;
            for (Edge edge : path.edges()) {
                double d = edge.segment().getDistanceMeters();
                length += d;
                weighted += d * SafetyScoreCalculator.weightedAverage(
                        SafetyScoreCalculator.typeScores(counts(edge.segment()), d), scoreWeights);
            }
            if (length == 0) {
                return 0.0;
            }
            return Math.round(weighted / length * 1000) / 10.0;
        }

        private Map<FacilityType, Integer> counts(RoadSegment segment) {
            return countCache.computeIfAbsent(segment.getId(), id -> edgeCostCalculator.facilityCounts(segment, index));
        }
    }
}
