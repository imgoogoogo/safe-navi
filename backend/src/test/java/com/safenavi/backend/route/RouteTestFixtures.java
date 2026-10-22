package com.safenavi.backend.route;

import java.lang.reflect.Field;
import java.util.List;

import com.safenavi.backend.common.geo.GeoUtils;
import com.safenavi.backend.common.geo.PolylineCodec;
import com.safenavi.backend.graphbuild.RoadNode;
import com.safenavi.backend.graphbuild.RoadSegment;

final class RouteTestFixtures {

    private RouteTestFixtures() {
    }

    static RoadNode node(long id, double lat, double lng) {
        return withId(new RoadNode(lat, lng, "11680"), id);
    }

    static RoadSegment segment(long id, RoadNode a, RoadNode b) {
        double d = GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
        String geometry = PolylineCodec.encode(List.of(new double[] {a.getLat(), a.getLng()},
                new double[] {b.getLat(), b.getLng()}));
        return withId(new RoadSegment(a, b, geometry, d, "11680"), id);
    }

    private static <T> T withId(T entity, long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
