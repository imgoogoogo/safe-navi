package com.safenavi.backend.report.dto;

import java.time.LocalDateTime;

import com.safenavi.backend.report.Report;

/** Own history, including reports an admin removed (status = REMOVED). */
public record MyReportResponse(Long id, String type, double lat, double lng, String address, String thumbnailUrl,
        String summary, String reviewStatus, String status, LocalDateTime createdAt) {

    public static MyReportResponse from(Report r) {
        return new MyReportResponse(r.getId(), r.getType(), r.getLat(), r.getLng(), r.getAddress(),
                r.getThumbnailUrl(), ReportSummaryResponse.summarize(r.getMemo()), r.getReviewStatus(),
                r.getStatus(), r.getCreatedAt());
    }
}
