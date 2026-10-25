package com.safenavi.backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    BBOX_TOO_LARGE(HttpStatus.BAD_REQUEST, "조회 범위가 너무 넓습니다."),
    UNKNOWN_PRESET(HttpStatus.BAD_REQUEST, "지원하지 않는 경로 프리셋입니다."),
    ROUTE_TOO_FAR(HttpStatus.BAD_REQUEST, "출발지와 도착지가 너무 멀리 떨어져 있습니다."),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다."),
    CANNOT_FLAG_OWN_REPORT(HttpStatus.BAD_REQUEST, "본인 제보는 신고할 수 없습니다."),
    GRAPH_SOURCE_NOT_FOUND(HttpStatus.BAD_REQUEST, "그래프 빌드용 OSM 파일을 찾을 수 없습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 계정입니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "제보를 찾을 수 없습니다."),
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "즐겨찾기를 찾을 수 없습니다."),
    NO_ROAD_DATA(HttpStatus.NOT_FOUND, "해당 지역의 도로 데이터가 없습니다."),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "경로를 찾을 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    ALREADY_FLAGGED(HttpStatus.CONFLICT, "이미 신고한 제보입니다."),
    SYNC_IN_PROGRESS(HttpStatus.CONFLICT, "공공데이터 동기화가 이미 실행 중입니다."),
    GRAPH_BUILD_IN_PROGRESS(HttpStatus.CONFLICT, "그래프 빌드가 이미 실행 중입니다."),

    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "파일 크기가 너무 큽니다."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 API 호출에 실패했습니다."),
    STORAGE_ERROR(HttpStatus.BAD_GATEWAY, "파일 저장소 처리에 실패했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
