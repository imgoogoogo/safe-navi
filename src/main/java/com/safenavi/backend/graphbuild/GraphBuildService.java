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

/**
 * 그래프 빌드 전체 흐름의 진입점. (호출: AdminController → POST /api/admin/graph-build/run)
 *
 * <pre>
 * [1] 요청 검증        pbf 경로 확인, clip 범위(선택) 확인, 동시 실행 방지
 * [2] 파싱            OsmPbfGraphReader.read()
 *                      ├─ 1차 패스: 걸을 수 있는 way 수집 + 노드 참조 횟수 세기 (WalkableWayFilter)
 *                      ├─ 2차 패스: 필요한 노드의 좌표만 수집 (clip 범위 밖은 버림)
 *                      └─ WaySplitter.split(): way를 교차점에서 잘라 RawSegment 목록 생성
 * [3] 저장            GraphPersister.replaceRegion()  ← 하나의 트랜잭션
 *                      ├─ 해당 regionCode의 기존 RoadSegment/RoadNode 삭제
 *                      ├─ RoadNode INSERT (OSM 노드 id → DB id 매핑 확보)
 *                      └─ RoadSegment INSERT (매핑된 DB id로 nodeAId/nodeBId 채움)
 * [4] 후처리          RegionCodeResolver 캐시 갱신 (지역 범위가 바뀌었으므로)
 * </pre>
 *
 * 파싱(메모리 작업)은 트랜잭션 밖에서, DB 쓰기만 트랜잭션 안에서 한다.
 * 파싱에 수십 초가 걸려도 그동안 DB 커넥션/락을 잡고 있지 않기 위해서다.
 */
@Service
public class GraphBuildService {

    private static final Logger log = LoggerFactory.getLogger(GraphBuildService.class);

    private final GraphBuildProperties properties;
    private final OsmPbfGraphReader reader;
    private final GraphPersister persister;
    private final RegionCodeResolver regionCodeResolver;
    // 빌드는 메모리를 많이 쓰고 같은 테이블을 지우고 쓰므로 한 번에 하나만 실행한다
    private final AtomicBoolean running = new AtomicBoolean(false);

    public GraphBuildService(GraphBuildProperties properties, OsmPbfGraphReader reader, GraphPersister persister,
            RegionCodeResolver regionCodeResolver) {
        this.properties = properties;
        this.reader = reader;
        this.persister = persister;
        this.regionCodeResolver = regionCodeResolver;
    }

    public GraphBuildResult run(GraphBuildRequest request) {
        // [1] 요청 검증 — 잘못된 요청은 실행 잠금을 잡기 전에 거른다
        Path pbf = resolvePbf(request.pbfPath());
        BoundingBox clip = clipBox(request);
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException(ErrorCode.GRAPH_BUILD_IN_PROGRESS);
        }
        long started = System.currentTimeMillis();
        try {
            log.info("Graph build started: region={}, pbf={}, clip={}", request.regionCode(), pbf, clip);

            // [2] 파싱: PBF → 메모리상의 RawSegment 목록 (OSM 노드 id 기준, 아직 DB id 없음)
            OsmPbfGraphReader.ReadResult read = reader.read(pbf, clip);

            // [3] 저장: 기존 지역 그래프를 지우고 새로 넣는다. 실패하면 롤백되어 이전 그래프가 유지된다
            GraphPersister.PersistResult saved = persister.replaceRegion(request.regionCode(), read.segments());

            // [4] 좌표 → regionCode 판정에 쓰는 지역 범위 캐시를 새 데이터로 갱신
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

    /** 요청의 pbfPath를 쓰고, 없으면 설정값(GRAPH_BUILD_PBF_PATH)을 쓴다. */
    private Path resolvePbf(String requested) {
        String path = requested != null && !requested.isBlank() ? requested : properties.pbfPath();
        if (path == null || path.isBlank()) {
            throw new BusinessException(ErrorCode.GRAPH_SOURCE_NOT_FOUND,
                    "pbfPath를 요청에 넣거나 GRAPH_BUILD_PBF_PATH 환경변수를 설정하세요.");
        }
        Path pbf = Path.of(path);
        // 관리자가 임의 경로를 넘길 수 있으므로 .pbf 파일만 허용
        if (!Files.isRegularFile(pbf) || !pbf.getFileName().toString().endsWith(".pbf")) {
            throw new BusinessException(ErrorCode.GRAPH_SOURCE_NOT_FOUND, "OSM .pbf 파일이 없습니다: " + path);
        }
        return pbf;
    }

    /** clip 범위는 네 값을 모두 주거나 모두 생략해야 한다. 생략하면 파일 전체를 빌드한다. */
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
