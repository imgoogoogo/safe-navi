package com.safenavi.backend.report;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportFlagRepository extends JpaRepository<ReportFlag, Long> {

    boolean existsByReportIdAndReporterId(Long reportId, Long reporterId);

    List<ReportFlag> findByReportIdIn(Collection<Long> reportIds);

    @Modifying
    @Query("delete from ReportFlag f where f.report.id = :reportId")
    int deleteByReportId(@Param("reportId") Long reportId);

    /** Active reports with at least one flag, most-flagged first. */
    @Query(value = """
            select new com.safenavi.backend.report.ReportFlagCount(f.report.id, count(f.id))
            from ReportFlag f
            where f.report.status = 'ACTIVE'
            group by f.report.id
            order by count(f.id) desc, f.report.id desc
            """,
            countQuery = "select count(distinct f.report.id) from ReportFlag f where f.report.status = 'ACTIVE'")
    Page<ReportFlagCount> findFlaggedActiveReports(Pageable pageable);
}
