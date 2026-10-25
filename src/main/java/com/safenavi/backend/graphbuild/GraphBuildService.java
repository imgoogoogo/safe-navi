package com.safenavi.backend.graphbuild;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.common.geo.BoundingBox;
import com.safenavi.backend.region.RegionCodeResolver;

/** Builds one region's walking graph from an OSM PBF extract. One build at a time. */
@Service
public class GraphBuildService {

    private static final Logger log = LoggerFactory.getLogger(GraphBuildService.class);

    private final GraphBuildProperties properties;
    private final OsmPbfGraphReader reader;
    private final GraphPersister persister;
    private final RegionCodeResolver regionCodeResolver;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public GraphBuildService(GraphBuildProperties properties, OsmPbfGraphReader reader, GraphPersister persister,
            RegionCodeResolver regionCodeResolver) {
        this.properties = properties;
        this.reader = reader;
        this.persister = persister;
        this.regionCodeResolver = regionCodeResolver;
    }

    public GraphBuildResult run(GraphBuildRequest request) {
        Path pbf = resolvePbf(request.pbfPath());
        BoundingBox clip = clipBox(request);
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException(ErrorCode.GRAPH_BUILD_IN_PROGRESS);
        }
        long started = System.currentTimeMillis();
        try {
            log.info("Graph build started: region={}, pbf={}, clip={}", request.regionCode(), pbf, clip);
            OsmPbfGraphReader.ReadResult read = reader.read(pbf, clip);
            GraphPersister.PersistResult saved = persister.replaceRegion(request.regionCode(), read.segments());
            regionCodeResolver.refresh();
            GraphBuildResult result = new GraphBuildResult(request.regionCode(), pbf.toString(), read.waysRead(),
                    saved.nodesSaved(), saved.segmentsSaved(), saved.nodesDeleted(), saved.segmentsDeleted(),
                    System.currentTimeMillis() - started);
            log.info("Graph build finished: {}", result);
            return result;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.GRAPH_SOURCE_NOT_FOUND, "OSM 파일을 읽을 수 없습니다: " + e.getMessage(), e);
        } finally {
            running.set(false);
        }
    }

    private Path resolvePbf(String requested) {
        String path = requested != null && !requested.isBlank() ? requested : properties.pbfPath();
        if (path == null || path.isBlank()) {
            throw new BusinessException(ErrorCode.GRAPH_SOURCE_NOT_FOUND,
                    "pbfPath를 요청에 넣거나 GRAPH_BUILD_PBF_PATH 환경변수를 설정하세요.");
        }
        Path pbf = Path.of(path);
        if (!Files.isRegularFile(pbf) || !pbf.getFileName().toString().endsWith(".pbf")) {
            throw new BusinessException(ErrorCode.GRAPH_SOURCE_NOT_FOUND, "OSM .pbf 파일이 없습니다: " + path);
        }
        return pbf;
    }

    private static BoundingBox clipBox(GraphBuildRequest r) {
        long given = Stream.of(r.swLat(), r.swLng(), r.neLat(), r.neLng()).filter(v -> v != null).count();
        if (given == 0) {
            return null;
        }
        if (given != 4) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "clip 범위는 swLat, swLng, neLat, neLng를 모두 보내야 합니다.");
        }
        return new BoundingBox(r.swLat(), r.swLng(), r.neLat(), r.neLng());
    }
}
