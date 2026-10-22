package com.safenavi.backend.graphbuild;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.collections.api.map.primitive.LongDoubleMap;
import org.eclipse.collections.api.map.primitive.LongIntMap;

import com.safenavi.backend.common.geo.GeoUtils;

/**
 * OSM way를 그래프 세그먼트로 자른다.
 *
 * <h3>왜 잘라야 하나</h3>
 * OSM의 way 하나는 교차로 여러 개를 지나는 긴 도로일 수 있다. 다익스트라는 "교차로에서 방향을 고른다"는
 * 모델이라, 그래프의 노드는 교차로(와 막다른 끝)여야 하고 간선은 교차로와 교차로 사이 구간이어야 한다.
 *
 * <h3>예시</h3>
 * <pre>
 *   way A: 1 ─ 2 ─ 3 ─ 4 ─ 5         refCounts: 3 → 2 (A, B 둘 다 사용), 나머지 → 1
 *   way B:     10 ─ 3 ─ 11
 *
 *   결과 세그먼트: 1→3 (좌표 1,2,3)   3→5 (좌표 3,4,5)   10→3   3→11
 *   그래프 노드:   1, 3, 5, 10, 11     (2와 4는 노드가 아니라 세그먼트 안의 "모양 점"으로만 남음)
 * </pre>
 * 모양 점(2, 4)을 geometry에 남겨 두기 때문에 지도에 그리면 실제 도로처럼 휘어진 선이 나온다.
 *
 * <h3>세그먼트를 끊는 지점</h3>
 * <ul>
 *   <li>refCounts &gt; 1: 다른 도로와 만나는 교차점 (같은 way 안에서 두 번 지나는 점도 포함)</li>
 *   <li>way의 마지막 노드</li>
 *   <li>다음 노드의 좌표가 없는 경우: clip 범위 밖이거나 추출 파일 경계라서, 도로가 여기서 끊긴다</li>
 * </ul>
 */
public final class WaySplitter {

    private WaySplitter() {
    }

    /**
     * @param ways      보행 가능한 way들의 노드 id 배열
     * @param refCounts 노드 id → 보행 way들에서 등장한 횟수
     * @param lats      노드 id → 위도 (clip 범위 안의 노드만 들어 있음)
     * @param lngs      노드 id → 경도
     */
    public static List<RawSegment> split(List<long[]> ways, LongIntMap refCounts, LongDoubleMap lats, LongDoubleMap lngs) {
        List<RawSegment> segments = new ArrayList<>();
        for (long[] way : ways) {
            long startId = -1;         // 지금 만들고 있는 세그먼트의 시작 노드
            boolean started = false;   // 세그먼트를 만드는 중인지 (좌표 없는 노드를 만나면 false로 리셋)
            long previousId = Long.MIN_VALUE;
            List<double[]> points = new ArrayList<>(); // 지금 세그먼트의 좌표들 [위도, 경도]

            for (int i = 0; i < way.length; i++) {
                long id = way[i];
                if (id == previousId) {
                    continue; // 같은 노드가 연속으로 두 번 나오는 데이터 오류는 건너뜀
                }
                previousId = id;

                // 좌표가 없는 노드(clip 밖) → 여기서 도로가 끊김. 모으던 좌표는 버리고 다음 노드부터 새로 시작
                if (!lats.containsKey(id)) {
                    started = false;
                    points = new ArrayList<>();
                    continue;
                }

                double[] point = {round7(lats.get(id)), round7(lngs.get(id))};

                // 세그먼트의 첫 점: 시작 노드로 기록하고 다음 노드로
                if (!started) {
                    started = true;
                    startId = id;
                    points = new ArrayList<>();
                    points.add(point);
                    continue;
                }

                points.add(point);
                boolean isLast = i == way.length - 1;
                boolean nextMissing = !isLast && !lats.containsKey(way[i + 1]);

                // 이 노드가 교차점이거나 도로의 끝이면 → 여기까지를 세그먼트 하나로 확정
                if (refCounts.get(id) > 1 || isLast || nextMissing) {
                    // 시작과 끝이 같은 노드면(교차점 없는 원형 도로) 의미 없는 자기 루프라 버린다
                    if (id != startId) {
                        segments.add(new RawSegment(startId, id, points, length(points)));
                    }
                    // 이 노드가 다음 세그먼트의 시작점이 된다
                    startId = id;
                    points = new ArrayList<>();
                    points.add(point);
                }
            }
        }
        return segments;
    }

    /** OSM 좌표 정밀도는 1e-7도. PBF 디코딩 때 생기는 37.498000000000005 같은 부동소수점 잡음을 없앤다. */
    private static double round7(double value) {
        return Math.round(value * 1e7) / 1e7;
    }

    /** 좌표를 차례로 이은 실제 길이(m). 직선거리가 아니라 모양 점을 따라간 길이다. */
    static double length(List<double[]> points) {
        double total = 0;
        for (int i = 1; i < points.size(); i++) {
            total += GeoUtils.haversineMeters(points.get(i - 1)[0], points.get(i - 1)[1], points.get(i)[0], points.get(i)[1]);
        }
        return total;
    }
}
