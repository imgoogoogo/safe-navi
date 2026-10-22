# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

SAFENAVI backend: safety-weighted walking navigation for Korea. Spring Boot 4.1.1 (Spring Framework 7, Security 7, Hibernate 7, **Jackson 3** — `tools.jackson.*` packages, annotations still `com.fasterxml.jackson.annotation`), Java 17, MySQL 8, Gradle 9 wrapper. Root package `com.safenavi.backend`.

## Commands

```bash
./gradlew build                 # compile + unit tests + bootJar
./gradlew test --tests 'com.safenavi.backend.route.DijkstraRouterTest'              # single class
./gradlew test --tests 'com.safenavi.backend.route.DijkstraRouterTest.unreachableReturnsEmpty'  # single method
./gradlew bootRun               # needs env vars below
```

Required env: `DB_PASSWORD`, `JWT_SECRET` (≥32 bytes). Common optional: `DB_URL`, `DB_USERNAME`, `DB_SCHEMA_INIT=always` (first run on an empty DB only — `schema.sql` uses plain `CREATE TABLE`), `PUBLIC_DATA_SERVICE_KEY`, `AWS_*`, `NOMINATIM_USER_AGENT`, `GRAPH_BUILD_PBF_PATH`. Secrets are only ever referenced from env in `application.yml`.

`SafeNaviApplicationTests` (full context) is skipped unless `DB_URL` is set, because `ddl-auto: validate` needs a real MySQL with `schema.sql` applied. Everything else is plain unit tests.

## Schema is the source of truth

`src/main/resources/schema.sql` defines the DB; Hibernate only validates (`ddl-auto: validate`). Tables/columns are PascalCase/camelCase, kept verbatim via `PhysicalNamingStrategyStandardImpl`. When changing an entity, change `schema.sql` to match or startup fails. `RoadSegment.geometry` is `TEXT` (mapped with `columnDefinition = "TEXT"`) holding a JSON array of `[lat, lng]` in nodeA→nodeB order (`PolylineCodec`). The road graph is undirected — keep the `nodeA`/`nodeB` naming.

Coordinates are deliberately plain `DOUBLE lat/lng`; the schema comment describes migrating to `POINT`/`SPATIAL INDEX` only once bbox queries are measured to be slow. Do not add `AccidentHotspot` or `PublicDataSyncLog` tables, and do not add JGraphT (Dijkstra is hand-written).

## Architecture

- **Responses/errors**: every controller returns `ApiResponse<T>{success,data,error}`. Throw `BusinessException(ErrorCode)`; `GlobalExceptionHandler` maps it. Security 401/403 go through `SecurityErrorResponder` in the same shape. Ownership checks (403) live in services, comparing the JWT `AuthUser.id()` to the resource owner.
- **Auth** (`auth.security`): stateless JWT (`typ` claim distinguishes access/refresh). `JwtAuthenticationFilter` is deliberately not a `@Component` and never rejects requests itself — public endpoints still see an optional principal (e.g. `isOwner` on report detail); the entry point reports why a token failed. Endpoint access rules are all in `SecurityConfig`; `/api/admin/**` requires `ROLE_ADMIN`. There is no admin signup: promote with `UPDATE User SET role='ADMIN'`.
- **Routing** (`route`): `RouteService` → `BoundingBoxCalculator` (500m margin) → `SubgraphProvider` (segments touching the bbox → adjacency map) → one `FacilityRepository.findByBoundingBox` (widened by the largest influence radius) → `FacilitySpatialIndex` (JTS STR-tree in local meters) → per preset `EdgeCostCalculator` + `DijkstraRouter`. Per-segment facility counts are computed once per request and reused across the 5 presets. Presets/weights are hardcoded in `PresetWeightResolver`.
- **Safety model** (`safety.SafetyScoreCalculator`): per facility type, count within that type's radius ÷ saturation count scaled by corridor area (radius/saturation per type in `facility.FacilityType`), clamped to [0,1]. Used by `/api/safety-score` and the reported route `safetyScore`. Edge costs do *not* use it: `EdgeCostCalculator` uses raw preset-weighted counts, `distance × (1 + 1 / (1 + Σ weight·count))` (no facility → 2× distance), so every extra facility keeps lowering the cost. Reported `safetyScore`s (routes and point) are 0–100 using the *default* preset weights so presets are comparable.
- **Regions**: `regionCode` comes from graph builds (admin supplies it per build). `RegionCodeResolver` maps a coordinate to a region via per-region node extents (approximate); used for facility sync and the realtime topic `/topic/alerts/{regionCode}` (STOMP endpoint `/ws`).
- **graphbuild**: `OsmPbfGraphReader` (osm4j, two passes, eclipse-collections primitive maps; `WalkableWayFilter` decides which OSM ways are walkable) → `WaySplitter` (cuts ways at shared nodes) → `GraphPersister` (JDBC batch, replaces one region in one transaction). Parsing happens outside the transaction; only the DB write is transactional. One build at a time (`GraphBuildService` guard). Triggered by `POST /api/admin/graph-build/run` with `{regionCode, pbfPath?, swLat..neLng?}` (clip box is all four values or none).
- **Graph debug map (temporary)**: `static/map-test.html` + `GraphDebugController` (`GET /api/debug/regions`, `/api/debug/graph`, bbox capped at 0.03°) are a smoke-test viewer for the built graph, made public in `SecurityConfig`. They are meant to be removed together: page, controller, `GraphDebugResponse`, and the `permitAll` line.
- **sync**: `PublicDataSyncService` pulls each enabled `public-data.sources[]` entry (JSON via Jackson, XML via JAXB `XmlResponse`), upserts on `externalId` with MySQL `INSERT … AS incoming ON DUPLICATE KEY UPDATE` (never overwrites `usedForRouting`), and deletes rows that vanished from a completely fetched source. Run nightly by `PublicDataSyncScheduler` and on demand by `POST /api/admin/public-data/sync`. Source field names (`lat-field`, etc.) are config, not code.
- **Reports**: S3 upload + Nominatim lookups happen outside DB transactions; uploaded objects are deleted if the DB write fails. Owner delete is a hard delete (flags first); admin `REMOVED` is a soft delete via `Report.status`. Report `type`/`reviewStatus` values are Korean strings (`ReportCodes`).
- **External calls**: Nominatim (`GeoService`) is throttled to 1 req/s with a small cache; a failed lookup leaves `Report.address` null instead of failing the report.
