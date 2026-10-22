package com.safenavi.backend.admin;

import java.time.LocalDateTime;
import java.util.List;

import com.safenavi.backend.report.Report;

public record AdminReportResponse(Long id, String type, double lat, double lng, String address, String imageUrl,
        String thumbnailUrl, String memo, String reviewStatus, String status, Long authorId, String authorName,
        long flagCount, List<String> flagReasons, LocalDateTime createdAt) {

    public static AdminReportResponse from(Report r, long flagCount, List<String> flagReasons) {
        return new AdminReportResponse(r.getId(), r.getType(), r.getLat(), r.getLng(), r.getAddress(),
                r.getImageUrl(), r.getThumbnailUrl(), r.getMemo(), r.getReviewStatus(), r.getStatus(),
                r.getUser().getId(), r.getUser().getName(), flagCount, flagReasons, r.getCreatedAt());
    }
}
