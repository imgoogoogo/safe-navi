-- =========================================================
-- SAFENAVI 데이터베이스 스키마 (MySQL 8.0, InnoDB)
-- ERD: SAFENAVI_ERD.mermaid / 명세: SAFENAVI_DB_설계명세서.docx
-- =========================================================

CREATE TABLE User (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_email (email)
);

CREATE TABLE RoadNode (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    regionCode VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_roadnode_region (regionCode)
);

CREATE TABLE RoadSegment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nodeAId BIGINT NOT NULL,
    nodeBId BIGINT NOT NULL,
    geometry TEXT NOT NULL,
    distanceMeters DOUBLE NOT NULL,
    regionCode VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_roadsegment_region (regionCode),
    CONSTRAINT fk_roadsegment_nodeA FOREIGN KEY (nodeAId) REFERENCES RoadNode (id),
    CONSTRAINT fk_roadsegment_nodeB FOREIGN KEY (nodeBId) REFERENCES RoadNode (id)
);

CREATE TABLE Facility (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(30) NOT NULL,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    usedForRouting BOOLEAN NOT NULL DEFAULT TRUE,
    externalId VARCHAR(100),
    sourceDataType VARCHAR(30) NOT NULL,
    regionCode VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_facility_externalid (externalId),
    KEY idx_facility_region (regionCode),
    KEY idx_facility_type (type),
    KEY idx_facility_latlng (lat, lng)
);

CREATE TABLE Report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    userId BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    address VARCHAR(255),
    imageUrl VARCHAR(500),
    thumbnailUrl VARCHAR(500),
    memo VARCHAR(500),
    reviewStatus VARCHAR(20) NOT NULL DEFAULT '접수됨',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_report_latlng (lat, lng),
    KEY idx_report_status (status),
    CONSTRAINT fk_report_user FOREIGN KEY (userId) REFERENCES User (id)
);

CREATE TABLE ReportFlag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reportId BIGINT NOT NULL,
    reporterId BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_reportflag_report FOREIGN KEY (reportId) REFERENCES Report (id),
    CONSTRAINT fk_reportflag_reporter FOREIGN KEY (reporterId) REFERENCES User (id)
);

CREATE TABLE Favorite (
    id BIGINT NOT NULL AUTO_INCREMENT,
    userId BIGINT NOT NULL,
    label VARCHAR(50) NOT NULL,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (userId) REFERENCES User (id)
);

-- ---------------------------------------------------------
-- 컬럼 설명 (참고용, 명세서 표와 동일)
--
-- User.role                   USER | ADMIN
-- User.status                 ACTIVE | SUSPENDED
-- Facility.type                cctv | streetlight | police | guardianHouse | emergencyMedical
-- Facility.usedForRouting     경로 가중치 반영 여부
-- Facility.externalId         공공데이터 원본 식별자 (중복 수집 방지)
-- Report.type                  조명부족 | 시야가림 | 기타
-- Report.reviewStatus          접수됨 | 검토중 | 반영됨
-- Report.status                ACTIVE | REMOVED (관리자 소프트 삭제)
-- RoadSegment.nodeAId/nodeBId  양 끝점 (무방향 그래프, 순서일 뿐 방향 아님)
-- regionCode                   시군구 코드, 전국 서비스의 파티셔닝/병렬 배치 기준
-- ---------------------------------------------------------

-- ---------------------------------------------------------
-- 참고: 공간 인덱스 마이그레이션 (개발 초기엔 보류, 성능 이슈 시 적용)
-- 전국 규모 서비스라 bbox·반경 쿼리에 SPATIAL INDEX가 결국 필요하지만,
-- 개발 초반엔 lat/lng DOUBLE 컬럼으로 가볍게 시작한다. 실제 데이터가
-- 쌓여서 findByBoundingBox() 조회가 느려지는 게 확인되면, 아래처럼
-- POINT 타입 + SPATIAL INDEX로 마이그레이션한다 (컬럼 타입 변경이라
-- 무중단 적용 시 별도 마이그레이션 스크립트로 관리):
--
-- ALTER TABLE Facility ADD COLUMN location POINT NOT NULL SRID 4326;
-- ALTER TABLE Facility ADD SPATIAL INDEX idx_facility_location (location);
-- (RoadNode, Report, Favorite도 동일하게 적용, RoadSegment.geometry는
--  TEXT 대신 LINESTRING으로 전환)
-- ---------------------------------------------------------
