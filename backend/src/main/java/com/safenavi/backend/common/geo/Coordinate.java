package com.safenavi.backend.common.geo;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;

public record Coordinate(double lat, double lng) {

    public Coordinate {
        if (Double.isNaN(lat) || Double.isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "좌표 범위가 올바르지 않습니다: " + lat + ", " + lng);
        }
    }
}
