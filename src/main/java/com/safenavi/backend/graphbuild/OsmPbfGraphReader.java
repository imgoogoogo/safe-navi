package com.safenavi.backend.graphbuild;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.collections.impl.map.mutable.primitive.LongDoubleHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;
import org.springframework.stereotype.Component;

import com.safenavi.backend.common.geo.BoundingBox;

import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;

/**
 * OSM PBF 파일을 읽어 보행 그래프 세그먼트(RawSegment) 목록을 만든다. 프로젝트에서 osm4j를 쓰는 유일한 곳.
 *
 * <h3>OSM 데이터 구조</h3>
 * <ul>
 *   <li>Node: id + 위도/경도. 도로의 꼭짓점도, 가게 위치 같은 점도 모두 Node다.</li>
 *   <li>Way: Node id의 순서 있는 목록 + 태그(highway=residential 등). 도로 하나가 Way 하나.</li>
 *   <li>Relation: 여러 요소의 묶음(버스 노선 등). 여기서는 쓰지 않는다.</li>
 * </ul>
 * PBF 파일 안에는 Node 전체 → Way 전체 → Relation 전체 순서로 저장되어 있다.
 *
 * <h3>왜 두 번 읽나</h3>
 * 한국 전체 PBF에는 노드가 수천만 개 있지만 보행 도로가 쓰는 노드는 그중 일부다.
 * 한 번에 읽으면 Way를 만나기 전에 모든 Node 좌표를 메모리에 올려야 하므로,
 * <ol>
 *   <li>1차 패스: Way만 보고 "어떤 노드가 필요한지"(refCounts)를 먼저 알아낸다.</li>
 *   <li>2차 패스: Node를 다시 읽으며 필요한 노드의 좌표만 저장한다.</li>
 * </ol>
 * 파일을 두 번 읽는 비용(수십 초)으로 메모리를 크게 줄인다.
 *
 * <h3>메모리</h3>
 * Map&lt;Long, Integer&gt;는 원소마다 박싱된 객체를 만들어 수백만 개 규모에서는 GB 단위로 커진다.
 * 그래서 eclipse-collections의 원시 타입 맵(LongIntHashMap, LongDoubleHashMap)을 쓴다.
 */
@Component
public class OsmPbfGraphReader {

    /**
     * @param waysRead 보행 가능으로 판정된 way 수 (clip과 무관하게 파일 전체 기준)
     * @param segments 교차점 단위로 잘린 세그먼트. 노드는 아직 OSM id로만 식별된다
     */
    public record ReadResult(int waysRead, List<RawSegment> segments) {
    }

    /**
     * @param clip null이면 파일 전체, 아니면 이 범위 안의 노드만 사용 (범위 밖 노드를 지나는 도로는 그 지점에서 끊긴다)
     */
    public ReadResult read(Path pbf, BoundingBox clip) throws IOException {
        // ───── 1차 패스: 보행 가능한 Way 수집 ─────
        // ways:      각 way의 노드 id 배열 (예: [101, 102, 103, 104])
        // refCounts: 노드 id → 보행 way들에서 등장한 횟수.
        //            2 이상이면 여러 도로가 만나는 교차점이라는 뜻이고, WaySplitter가 이 값으로 도로를 자른다.
        List<long[]> ways = new ArrayList<>();
        LongIntHashMap refCounts = new LongIntHashMap();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(pbf))) {
            // PbfIterator(in, false): false = 작성자/버전 같은 메타데이터는 읽지 않음 (속도·메모리 절약)
            for (EntityContainer container : new PbfIterator(in, false)) {
                if (container.getType() != EntityType.Way) {
                    continue; // Node/Relation은 이번 패스에서는 무시
                }
                OsmWay way = (OsmWay) container.getEntity();
                // 점 하나짜리 way나, 고속도로·사유지·건물 등 보행 불가 way는 제외
                if (way.getNumberOfNodes() < 2 || !WalkableWayFilter.isWalkable(OsmModelUtil.getTagsAsMap(way))) {
                    continue;
                }
                long[] nodeIds = new long[way.getNumberOfNodes()];
                for (int i = 0; i < nodeIds.length; i++) {
                    nodeIds[i] = way.getNodeId(i);
                    refCounts.addToValue(nodeIds[i], 1); // 없으면 0에서 시작해 +1
                }
                ways.add(nodeIds);
            }
        }

        // ───── 2차 패스: 필요한 Node의 좌표만 수집 ─────
        // 위도/경도를 double[] 객체로 두면 노드마다 객체가 생기므로 원시 맵 두 개로 나눠 저장한다
        LongDoubleHashMap lats = new LongDoubleHashMap();
        LongDoubleHashMap lngs = new LongDoubleHashMap();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(pbf))) {
            for (EntityContainer container : new PbfIterator(in, false)) {
                if (container.getType() == EntityType.Way) {
                    break; // PBF는 Node가 모두 끝난 뒤 Way가 나오므로, Way를 만나면 더 읽을 필요가 없다
                }
                if (container.getType() != EntityType.Node) {
                    continue;
                }
                OsmNode node = (OsmNode) container.getEntity();
                if (!refCounts.containsKey(node.getId())) {
                    continue; // 보행 도로가 쓰지 않는 노드 (POI, 건물 꼭짓점 등)
                }
                if (clip != null && !clip.contains(node.getLatitude(), node.getLongitude())) {
                    continue; // clip 범위 밖. 좌표가 없는 노드는 WaySplitter에서 "도로가 끊긴 지점"으로 처리된다
                }
                lats.put(node.getId(), node.getLatitude());
                lngs.put(node.getId(), node.getLongitude());
            }
        }

        // ───── 도로를 교차점에서 잘라 세그먼트로 변환 ─────
        return new ReadResult(ways.size(), WaySplitter.split(ways, refCounts, lats, lngs));
    }
}
