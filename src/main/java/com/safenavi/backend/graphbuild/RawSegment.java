package com.safenavi.backend.graphbuild;

import java.util.List;

/**
 * 파싱 단계의 중간 결과: 두 그래프 노드 사이의 도로 구간. 아직 DB에 저장되기 전이라 노드를 OSM id로 가리킨다.
 *
 * @param points         [위도, 경도] 목록. 첫 점 = osmNodeA 위치, 마지막 점 = osmNodeB 위치, 중간은 도로 모양 점
 * @param distanceMeters points를 차례로 이은 길이(m)
 */
public record RawSegment(long osmNodeA, long osmNodeB, List<double[]> points, double distanceMeters) {
}
