package com.safenavi.backend.safety;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;

import com.safenavi.backend.common.geo.LocalProjection;
import com.safenavi.backend.facility.Facility;
import com.safenavi.backend.facility.FacilityType;

/**
 * Facilities of one query area in an STR-tree, in local planar meters, so "how many facilities of
 * each type lie within r of this geometry" does not scan every facility for every road segment.
 */
public class FacilitySpatialIndex {

    private final LocalProjection projection;
    private final GeometryFactory geometryFactory;
    private final STRtree tree = new STRtree();

    private record Entry(FacilityType type, Point point) {
    }

    public FacilitySpatialIndex(List<Facility> facilities, LocalProjection projection, GeometryFactory geometryFactory) {
        this.projection = projection;
        this.geometryFactory = geometryFactory;
        for (Facility f : facilities) {
            FacilityType.fromCode(f.getType()).ifPresent(type -> {
                Point p = geometryFactory.createPoint(projection.toMeters(f.getLat(), f.getLng()));
                tree.insert(p.getEnvelopeInternal(), new Entry(type, p));
            });
        }
        tree.build();
    }

    public LocalProjection getProjection() {
        return projection;
    }

    public GeometryFactory getGeometryFactory() {
        return geometryFactory;
    }

    /** Per-type count of facilities within that type's influence radius of the geometry. */
    public Map<FacilityType, Integer> countWithinInfluence(Geometry geometryMeters) {
        Map<FacilityType, Integer> counts = new EnumMap<>(FacilityType.class);
        for (FacilityType type : FacilityType.values()) {
            counts.put(type, count(geometryMeters, type, type.getInfluenceRadiusMeters()));
        }
        return counts;
    }

    public int count(Geometry geometryMeters, FacilityType type, double radiusMeters) {
        Envelope search = new Envelope(geometryMeters.getEnvelopeInternal());
        search.expandBy(radiusMeters);
        int count = 0;
        for (Object o : tree.query(search)) {
            Entry e = (Entry) o;
            if (e.type() == type && geometryMeters.isWithinDistance(e.point(), radiusMeters)) {
                count++;
            }
        }
        return count;
    }
}
