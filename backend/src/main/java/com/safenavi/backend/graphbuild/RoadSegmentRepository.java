package com.safenavi.backend.graphbuild;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoadSegmentRepository extends JpaRepository<RoadSegment, Long> {

    /** Segments with at least one end inside the box, with both end nodes fetched. */
    @Query("""
            select s from RoadSegment s join fetch s.nodeA a join fetch s.nodeB b
            where (a.lat between :swLat and :neLat and a.lng between :swLng and :neLng)
               or (b.lat between :swLat and :neLat and b.lng between :swLng and :neLng)
            """)
    List<RoadSegment> findByBoundingBox(@Param("swLat") double swLat, @Param("swLng") double swLng,
            @Param("neLat") double neLat, @Param("neLng") double neLng);
}
