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
 * Replaces one region's RoadNode/RoadSegment rows in a single transaction, so a failed build
 * leaves the previous graph in place. Uses JDBC batches: JPA IDENTITY inserts cannot be batched.
 */
@Component
public class GraphPersister {

    private static final int BATCH_SIZE = 2000;

    public record PersistResult(int nodesSaved, int segmentsSaved, int nodesDeleted, int segmentsDeleted) {
    }

    private final JdbcTemplate jdbcTemplate;

    public GraphPersister(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public PersistResult replaceRegion(String regionCode, List<RawSegment> segments) {
        int segmentsDeleted = jdbcTemplate.update("DELETE FROM RoadSegment WHERE regionCode = ?", regionCode);
        int nodesDeleted = jdbcTemplate.update("DELETE FROM RoadNode WHERE regionCode = ?", regionCode);

        LongLongHashMap dbIds = insertNodes(regionCode, segments);
        insertSegments(regionCode, segments, dbIds);
        return new PersistResult(dbIds.size(), segments.size(), nodesDeleted, segmentsDeleted);
    }

    /** Inserts each distinct segment end once; returns OSM node id -> RoadNode.id. */
    private LongLongHashMap insertNodes(String regionCode, List<RawSegment> segments) {
        LongLongHashMap dbIds = new LongLongHashMap();
        List<long[]> pendingOsmIds = new ArrayList<>();
        List<double[]> pendingCoords = new ArrayList<>();
        for (RawSegment s : segments) {
            queueNode(s.osmNodeA(), s.points().get(0), dbIds, pendingOsmIds, pendingCoords);
            queueNode(s.osmNodeB(), s.points().get(s.points().size() - 1), dbIds, pendingOsmIds, pendingCoords);
        }
        for (int from = 0; from < pendingOsmIds.size(); from += BATCH_SIZE) {
            int to = Math.min(pendingOsmIds.size(), from + BATCH_SIZE);
            List<long[]> ids = pendingOsmIds.subList(from, to);
            List<double[]> coords = pendingCoords.subList(from, to);
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
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        int i = 0;
                        while (keys.next()) {
                            dbIds.put(ids.get(i++)[0], keys.getLong(1));
                        }
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

    private static void queueNode(long osmId, double[] coord, LongLongHashMap dbIds, List<long[]> pendingOsmIds,
            List<double[]> pendingCoords) {
        if (!dbIds.containsKey(osmId)) {
            dbIds.put(osmId, -1L); // placeholder until the generated id is known
            pendingOsmIds.add(new long[] {osmId});
            pendingCoords.add(coord);
        }
    }

    private void insertSegments(String regionCode, List<RawSegment> segments, LongLongHashMap dbIds) {
        String sql = "INSERT INTO RoadSegment (nodeAId, nodeBId, geometry, distanceMeters, regionCode) VALUES (?, ?, ?, ?, ?)";
        for (int from = 0; from < segments.size(); from += BATCH_SIZE) {
            List<RawSegment> chunk = segments.subList(from, Math.min(segments.size(), from + BATCH_SIZE));
            jdbcTemplate.batchUpdate(sql, chunk, chunk.size(), (ps, s) -> {
                ps.setLong(1, dbIds.get(s.osmNodeA()));
                ps.setLong(2, dbIds.get(s.osmNodeB()));
                ps.setString(3, PolylineCodec.encode(s.points()));
                ps.setDouble(4, Math.round(s.distanceMeters() * 100) / 100.0);
                ps.setString(5, regionCode);
            });
        }
    }
}
