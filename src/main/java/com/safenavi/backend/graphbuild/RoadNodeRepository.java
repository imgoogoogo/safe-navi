package com.safenavi.backend.graphbuild;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoadNodeRepository extends JpaRepository<RoadNode, Long> {

    @Query("""
            select n from RoadNode n
            where n.lat between :swLat and :neLat and n.lng between :swLng and :neLng
            """)
    List<RoadNode> findByBoundingBox(@Param("swLat") double swLat, @Param("swLng") double swLng,
            @Param("neLat") double neLat, @Param("neLng") double neLng);

    /** Per-region extent, used to map a coordinate to a regionCode. */
    @Query("""
            select new com.safenavi.backend.region.RegionExtent(n.regionCode, min(n.lat), min(n.lng), max(n.lat), max(n.lng))
            from RoadNode n group by n.regionCode
            """)
    List<com.safenavi.backend.region.RegionExtent> findRegionExtents();
}
