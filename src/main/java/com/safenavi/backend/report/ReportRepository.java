package com.safenavi.backend.report;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("""
            select r from Report r
            where r.status = 'ACTIVE'
              and r.lat between :swLat and :neLat and r.lng between :swLng and :neLng
              and (:type is null or r.type = :type)
              and (:reviewStatus is null or r.reviewStatus = :reviewStatus)
            order by r.createdAt desc
            """)
    List<Report> findActiveInBox(@Param("swLat") double swLat, @Param("swLng") double swLng,
            @Param("neLat") double neLat, @Param("neLng") double neLng,
            @Param("type") String type, @Param("reviewStatus") String reviewStatus, Pageable limit);

    @EntityGraph(attributePaths = "user")
    Optional<Report> findWithUserById(Long id);

    Page<Report> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    List<Report> findWithUserByIdIn(Collection<Long> ids);
}
