package com.safenavi.backend.graphbuild;

import java.util.Map;
import java.util.Set;

/**
 * OSM way의 태그를 보고 사람이 걸을 수 있는 도로인지 판정한다.
 *
 * 판정 순서:
 * <ol>
 *   <li>highway 태그가 없으면 도로가 아님 (건물, 강, 행정경계 등) → 제외</li>
 *   <li>area=yes면 광장 같은 면(polygon)이라 선형 도로로 쓸 수 없음 → 제외</li>
 *   <li>foot=no/private → 보행 금지 → 제외</li>
 *   <li>foot=yes/designated/permissive → 보행 명시 허용 → 자동차 전용도로 등급이어도 포함</li>
 *   <li>access=no/private → 통행 금지(사유지 등) → 제외</li>
 *   <li>그 외에는 highway 값이 보행 가능 목록에 있으면 포함</li>
 * </ol>
 * 태그 의미: https://wiki.openstreetmap.org/wiki/Key:highway
 */
public final class WalkableWayFilter {

    /** 기본적으로 걸을 수 있는 highway 값. 보도·골목·일반 도로(인도가 있다고 가정)를 포함한다. */
    private static final Set<String> WALKABLE_HIGHWAYS = Set.of(
            "footway", "pedestrian", "path", "steps", "living_street", "residential", "service",
            "unclassified", "track", "cycleway", "corridor", "crossing",
            "tertiary", "tertiary_link", "secondary", "secondary_link", "primary", "primary_link", "road");
    /** 자동차 전용도로(고속도로 등). foot=yes처럼 보행이 명시된 경우에만 포함한다. */
    private static final Set<String> FOOT_TAG_REQUIRED = Set.of("trunk", "trunk_link", "motorway", "motorway_link");
    private static final Set<String> FOOT_ALLOWED = Set.of("yes", "designated", "permissive");
    private static final Set<String> NO_ACCESS = Set.of("no", "private");

    private WalkableWayFilter() {
    }

    public static boolean isWalkable(Map<String, String> tags) {
        String highway = tags.get("highway");
        if (highway == null || "yes".equals(tags.get("area"))) {
            return false;
        }
        return isWalkableHighway(highway, tags);
    }

    private static boolean isWalkableHighway(String highway, Map<String, String> tags) {
        String foot = tags.get("foot");
        // 보행자 전용 태그(foot)가 일반 통행 태그(access)보다 우선한다
        if (foot != null && NO_ACCESS.contains(foot)) {
            return false;
        }
        if (foot != null && FOOT_ALLOWED.contains(foot)) {
            return WALKABLE_HIGHWAYS.contains(highway) || FOOT_TAG_REQUIRED.contains(highway);
        }
        String access = tags.get("access");
        if (access != null && NO_ACCESS.contains(access)) {
            return false;
        }
        return WALKABLE_HIGHWAYS.contains(highway);
    }
}
