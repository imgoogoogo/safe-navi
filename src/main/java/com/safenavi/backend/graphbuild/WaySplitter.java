package com.safenavi.backend.graphbuild;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.collections.api.map.primitive.LongDoubleMap;
import org.eclipse.collections.api.map.primitive.LongIntMap;

import com.safenavi.backend.common.geo.GeoUtils;

/**
 * Turns OSM ways into graph segments. A way is cut at every node shared with another way (or
 * repeated within itself) so intersections become graph nodes; intermediate shape points stay in
 * the segment geometry. Nodes without coordinates (outside the extract or clip box) end the current
 * piece, so partially clipped ways keep their inside parts.
 */
public final class WaySplitter {

    private WaySplitter() {
    }

    public static List<RawSegment> split(List<long[]> ways, LongIntMap refCounts, LongDoubleMap lats, LongDoubleMap lngs) {
        List<RawSegment> segments = new ArrayList<>();
        for (long[] way : ways) {
            long startId = -1;
            boolean started = false;
            long previousId = Long.MIN_VALUE;
            List<double[]> points = new ArrayList<>();
            for (int i = 0; i < way.length; i++) {
                long id = way[i];
                if (id == previousId) {
                    continue; // duplicated consecutive node
                }
                previousId = id;
                if (!lats.containsKey(id)) {
                    started = false;
                    points = new ArrayList<>();
                    continue;
                }
                double[] point = {round7(lats.get(id)), round7(lngs.get(id))};
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
                if (refCounts.get(id) > 1 || isLast || nextMissing) {
                    if (id != startId) {
                        segments.add(new RawSegment(startId, id, points, length(points)));
                    }
                    startId = id;
                    points = new ArrayList<>();
                    points.add(point);
                }
            }
        }
        return segments;
    }

    /** OSM stores coordinates at 1e-7 degrees; drop the float noise PBF decoding adds. */
    private static double round7(double value) {
        return Math.round(value * 1e7) / 1e7;
    }

    static double length(List<double[]> points) {
        double total = 0;
        for (int i = 1; i < points.size(); i++) {
            total += GeoUtils.haversineMeters(points.get(i - 1)[0], points.get(i - 1)[1], points.get(i)[0], points.get(i)[1]);
        }
        return total;
    }
}
