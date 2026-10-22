package com.safenavi.backend.graphbuild;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safenavi.backend.common.geo.BoundingBox;
import com.safenavi.backend.common.geo.PolylineCodec;
import com.safenavi.backend.common.response.ApiResponse;
import com.safenavi.backend.graphbuild.GraphDebugResponse.NodeView;
import com.safenavi.backend.graphbuild.GraphDebugResponse.SegmentView;
import com.safenavi.backend.region.RegionExtent;

/** Debug view of the stored road graph for static/map-test.html; remove together with that page. */
@RestController
public class GraphDebugController {

    /** About 3km x 2.5km: enough for a zoom-15 viewport, small enough to render every segment. */
    private static final double MAX_SPAN_DEGREES = 0.03;

    private final RoadSegmentRepository roadSegmentRepository;
    private final RoadNodeRepository roadNodeRepository;

    public GraphDebugController(RoadSegmentRepository roadSegmentRepository, RoadNodeRepository roadNodeRepository) {
        this.roadSegmentRepository = roadSegmentRepository;
        this.roadNodeRepository = roadNodeRepository;
    }

    /** 그래프가 빌드된 지역 목록과 각 지역 노드의 최소/최대 좌표 범위. */
    @GetMapping("/api/debug/regions")
    @Transactional(readOnly = true)
    public ApiResponse<List<RegionExtent>> regions() {
        return ApiResponse.ok(roadNodeRepository.findRegionExtents());
    }

    @GetMapping("/api/debug/graph")
    @Transactional(readOnly = true)
    public ApiResponse<GraphDebugResponse> graph(@RequestParam double swLat, @RequestParam double swLng,
            @RequestParam double neLat, @RequestParam double neLng) {
        BoundingBox box = new BoundingBox(swLat, swLng, neLat, neLng).requireMaxSpan(MAX_SPAN_DEGREES);
        List<RoadSegment> segments = roadSegmentRepository.findByBoundingBox(swLat, swLng, neLat, neLng);

        Map<Long, Integer> degree = new HashMap<>();
        Map<Long, RoadNode> nodes = new LinkedHashMap<>();
        for (RoadSegment s : segments) {
            for (RoadNode n : List.of(s.getNodeA(), s.getNodeB())) {
                degree.merge(n.getId(), 1, Integer::sum);
                if (box.contains(n.getLat(), n.getLng())) {
                    nodes.putIfAbsent(n.getId(), n);
                }
            }
        }
        List<NodeView> nodeViews = nodes.values().stream()
                .map(n -> new NodeView(n.getId(), n.getLat(), n.getLng(), degree.get(n.getId())))
                .toList();
        List<SegmentView> segmentViews = segments.stream()
                .map(s -> new SegmentView(s.getId(), s.getNodeA().getId(), s.getNodeB().getId(), s.getDistanceMeters(),
                        PolylineCodec.decode(s.getGeometry())))
                .toList();
        return ApiResponse.ok(new GraphDebugResponse(nodeViews, segmentViews));
    }
}
