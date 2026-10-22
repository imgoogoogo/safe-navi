package com.safenavi.backend.report.dto;

import java.time.LocalDateTime;

import com.safenavi.backend.report.Report;

/** List view: thumbnail and a memo excerpt only, never the original image or full memo. */
public record ReportSummaryResponse(Long id, String type, double lat, double lng, String thumbnailUrl,
        String summary, String reviewStatus, LocalDateTime createdAt) {

    static final int SUMMARY_LENGTH = 40;

    public static ReportSummaryResponse from(Report r) {
        return new ReportSummaryResponse(r.getId(), r.getType(), r.getLat(), r.getLng(), r.getThumbnailUrl(),
                summarize(r.getMemo()), r.getReviewStatus(), r.getCreatedAt());
    }

    public static String summarize(String memo) {
        if (memo == null || memo.isBlank()) {
            return null;
        }
        String trimmed = memo.strip();
        if (trimmed.codePointCount(0, trimmed.length()) <= SUMMARY_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, trimmed.offsetByCodePoints(0, SUMMARY_LENGTH)) + "…";
    }
}
