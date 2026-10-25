package com.safenavi.backend.graphbuild;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.collections.impl.map.mutable.primitive.LongLongHashMap;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.safenavi.backend.common.geo.PolylineCodec;

/**
 * 파싱 결과(RawSegment)를 RoadNode/RoadSegment 테이블에 저장한다.
 *
 * <h3>핵심 문제: OSM id → DB id 변환</h3>
 * RawSegment는 양 끝을 OSM 노드 id(예: 4662808659)로 가리키지만, RoadSegment.nodeAId/nodeBId는
 * RoadNode.id(AUTO_INCREMENT로 DB가 발급)를 가리키는 FK다. 그래서 순서가 정해져 있다.
 * <ol>
 *   <li>RoadNode를 먼저 INSERT하고, DB가 발급한 id를 받아 "OSM id → DB id" 맵을 만든다.</li>
 *   <li>그 맵으로 FK를 채워 RoadSegment를 INSERT한다.</li>
 * </ol>
 *
 * <h3>왜 JPA(save) 대신 JDBC인가</h3>
 * 엔티티가 IDENTITY 전략이라 Hibernate는 INSERT를 하나씩 즉시 실행한다(배치 불가).
 * 수만~수백만 행을 넣어야 하므로 JDBC 배치를 쓴다. 접속 URL의 rewriteBatchedStatements=true 덕분에
 * MySQL 드라이버가 배치를 여러 행짜리 INSERT 한 문장으로 바꿔 보내서 훨씬 빠르다.
 *
 * <h3>트랜잭션</h3>
 * 삭제 + 노드 INSERT + 세그먼트 INSERT가 모두 한 트랜잭션이다(JdbcTemplate도 @Transactional에 참여).
 * 중간에 실패하면 전부 롤백되어 해당 지역의 기존 그래프가 그대로 남는다.
 */
@Component
public class GraphPersister {

    /** 한 번에 DB로 보내는 행 수. 너무 크면 패킷 한도(max_allowed_packet)에 걸리고, 너무 작으면 왕복이 많아진다. */
    private static final int BATCH_SIZE = 2000;

    public record PersistResult(int nodesSaved, int segmentsSaved, int nodesDeleted, int segmentsDeleted) {
    }

    private final JdbcTemplate jdbcTemplate;

    public GraphPersister(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public PersistResult replaceRegion(String regionCode, List<RawSegment> segments) {
        // ① 같은 지역을 다시 빌드하면 교체되도록 기존 데이터를 지운다.
        //    RoadSegment가 RoadNode를 FK로 참조하므로 세그먼트를 먼저 지워야 한다.
        int segmentsDeleted = jdbcTemplate.update("DELETE FROM RoadSegment WHERE regionCode = ?", regionCode);
        int nodesDeleted = jdbcTemplate.update("DELETE FROM RoadNode WHERE regionCode = ?", regionCode);

        // ② 노드 저장 → OSM id → DB id 맵
        LongLongHashMap dbIds = insertNodes(regionCode, segments);
        // ③ 맵으로 FK를 채워 세그먼트 저장
        insertSegments(regionCode, segments, dbIds);
        return new PersistResult(dbIds.size(), segments.size(), nodesDeleted, segmentsDeleted);
    }

    /**
     * 세그먼트 양 끝 노드를 중복 없이 한 번씩 INSERT하고, OSM 노드 id → RoadNode.id 맵을 돌려준다.
     * 교차점 노드는 여러 세그먼트의 끝점이지만 RoadNode 행은 하나만 만든다.
     */
    private LongLongHashMap insertNodes(String regionCode, List<RawSegment> segments) {
        LongLongHashMap dbIds = new LongLongHashMap();
        // INSERT할 노드를 순서대로 모은다. 두 리스트의 i번째는 같은 노드다 (OSM id / 좌표)
        List<long[]> pendingOsmIds = new ArrayList<>();
        List<double[]> pendingCoords = new ArrayList<>();
        for (RawSegment s : segments) {
            // 세그먼트의 첫 좌표 = nodeA 위치, 마지막 좌표 = nodeB 위치
            queueNode(s.osmNodeA(), s.points().get(0), dbIds, pendingOsmIds, pendingCoords);
            queueNode(s.osmNodeB(), s.points().get(s.points().size() - 1), dbIds, pendingOsmIds, pendingCoords);
        }

        // BATCH_SIZE개씩 잘라서 INSERT
        for (int from = 0; from < pendingOsmIds.size(); from += BATCH_SIZE) {
            int to = Math.min(pendingOsmIds.size(), from + BATCH_SIZE);
            List<long[]> ids = pendingOsmIds.subList(from, to);
            List<double[]> coords = pendingCoords.subList(from, to);
            // 생성된 키(AUTO_INCREMENT id)를 받아야 해서 JdbcTemplate.batchUpdate 대신 Connection을 직접 쓴다.
            // ConnectionCallback으로 받은 커넥션은 현재 트랜잭션의 커넥션이다.
            jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO RoadNode (lat, lng, regionCode) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    for (double[] c : coords) {
                        ps.setDouble(1, c[0]);
                        ps.setDouble(2, c[1]);
                        ps.setString(3, regionCode);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    // 발급된 id는 INSERT한 순서대로 돌아온다 → i번째 키 = i번째 OSM 노드의 DB id
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        int i = 0;
                        while (keys.next()) {
                            dbIds.put(ids.get(i++)[0], keys.getLong(1));
                        }
                        // 개수가 어긋나면 FK가 엉뚱한 노드를 가리키게 되므로 즉시 실패시켜 롤백한다
                        if (i != ids.size()) {
                            throw new IllegalStateException("Expected " + ids.size() + " generated keys, got " + i);
                        }
                    }
                }
                return null;
            });
        }
        return dbIds;
    }

    /** 처음 보는 OSM 노드면 INSERT 대기열에 넣는다. */
    private static void queueNode(long osmId, double[] coord, LongLongHashMap dbIds, List<long[]> pendingOsmIds,
            List<double[]> pendingCoords) {
        if (!dbIds.containsKey(osmId)) {
            dbIds.put(osmId, -1L); // 중복 방지용 임시값. insertNodes에서 실제 DB id로 덮어쓴다
            pendingOsmIds.add(new long[] {osmId});
            pendingCoords.add(coord);
        }
    }

    /** 세그먼트를 INSERT한다. 양 끝은 OSM id가 아니라 insertNodes가 만든 맵으로 찾은 DB id로 저장한다. */
    private void insertSegments(String regionCode, List<RawSegment> segments, LongLongHashMap dbIds) {
        String sql = "INSERT INTO RoadSegment (nodeAId, nodeBId, geometry, distanceMeters, regionCode) VALUES (?, ?, ?, ?, ?)";
        for (int from = 0; from < segments.size(); from += BATCH_SIZE) {
            List<RawSegment> chunk = segments.subList(from, Math.min(segments.size(), from + BATCH_SIZE));
            jdbcTemplate.batchUpdate(sql, chunk, chunk.size(), (ps, s) -> {
                ps.setLong(1, dbIds.get(s.osmNodeA()));
                ps.setLong(2, dbIds.get(s.osmNodeB()));
                // geometry: [[위도,경도], ...] JSON 문자열. nodeA → nodeB 순서 (경로 탐색 시 역방향이면 뒤집어 씀)
                ps.setString(3, PolylineCodec.encode(s.points()));
                ps.setDouble(4, Math.round(s.distanceMeters() * 100) / 100.0); // cm 단위로 반올림
                ps.setString(5, regionCode);
            });
        }
    }
}
