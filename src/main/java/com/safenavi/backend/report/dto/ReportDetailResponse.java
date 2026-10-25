package com.safenavi.backend.report.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.safenavi.backend.report.Report;

public record ReportDetailResponse(Long id, String type, double lat, double lng, String address, String imageUrl,
        String thumbnailUrl, String memo, String reviewStatus, String authorName,
        @JsonProperty("isOwner") boolean isOwner, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static ReportDetailResponse from(Report r, Long requesterId) {
        return new ReportDetailResponse(r.getId(), r.getType(), r.getLat(), r.getLng(), r.getAddress(),
                r.getImageUrl(), r.getThumbnailUrl(), r.getMemo(), r.getReviewStatus(), r.getUser().getName(),
                r.isOwnedBy(requesterId), r.getCreatedAt(), r.getUpdatedAt());
    }
}
