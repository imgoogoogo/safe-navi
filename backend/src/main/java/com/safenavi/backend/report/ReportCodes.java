package com.safenavi.backend.report;

import java.util.List;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;

/** Korean code values stored in Report.type / Report.reviewStatus (see schema.sql). */
public final class ReportCodes {

    public static final List<String> TYPES = List.of("조명부족", "시야가림", "기타");

    public static final String REVIEW_RECEIVED = "접수됨";
    public static final List<String> REVIEW_STATUSES = List.of(REVIEW_RECEIVED, "검토중", "반영됨");

    private ReportCodes() {
    }

    public static String requireType(String type) {
        if (type == null || !TYPES.contains(type.trim())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "제보 유형은 " + TYPES + " 중 하나여야 합니다.");
        }
        return type.trim();
    }

    public static String requireReviewStatus(String reviewStatus) {
        if (!REVIEW_STATUSES.contains(reviewStatus)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "검토 상태는 " + REVIEW_STATUSES + " 중 하나여야 합니다.");
        }
        return reviewStatus;
    }
}
