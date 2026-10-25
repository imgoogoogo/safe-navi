package com.safenavi.backend.facility;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    @Query("""
            select f from Facility f
            where f.lat between :swLat and :neLat and f.lng between :swLng and :neLng
            """)
    List<Facility> findByBoundingBox(@Param("swLat") double swLat, @Param("swLng") double swLng,
            @Param("neLat") double neLat, @Param("neLng") double neLng);

    @Query("""
            select f from Facility f
            where f.lat between :swLat and :neLat and f.lng between :swLng and :neLng and f.type = :type
            """)
    List<Facility> findByBoundingBoxAndType(@Param("swLat") double swLat, @Param("swLng") double swLng,
            @Param("neLat") double neLat, @Param("neLng") double neLng, @Param("type") String type);

    @Query("select f.externalId from Facility f where f.sourceDataType = :source and f.externalId is not null")
    List<String> findExternalIdsBySource(@Param("source") String sourceDataType);
}
