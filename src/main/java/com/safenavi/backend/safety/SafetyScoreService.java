package com.safenavi.backend.safety;

import java.util.EnumMap;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.common.geo.BoundingBox;
import com.safenavi.backend.common.geo.Coordinate;
import com.safenavi.backend.common.geo.GeoUtils;
import com.safenavi.backend.common.geo.LocalProjection;
import com.safenavi.backend.facility.Facility;
import com.safenavi.backend.facility.FacilityRepository;
import com.safenavi.backend.facility.FacilityType;
import com.safenavi.backend.route.PresetWeightResolver;

@Service
public class SafetyScoreService {

    public static final double DEFAULT_RADIUS_METERS = 100;
    private static final double MAX_RADIUS_METERS = 2000;

    private final FacilityRepository facilityRepository;
    private final PresetWeightResolver presetWeightResolver;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public SafetyScoreService(FacilityRepository facilityRepository, PresetWeightResolver presetWeightResolver) {
        this.facilityRepository = facilityRepository;
        this.presetWeightResolver = presetWeightResolver;
    }

    @Transactional(readOnly = true)
    public SafetyScoreResponse score(Coordinate point, double radiusMeters) {
        if (radiusMeters <= 0 || radiusMeters > MAX_RADIUS_METERS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "radius는 0 초과 " + (int) MAX_RADIUS_METERS + " 이하여야 합니다.");
        }
        BoundingBox box = GeoUtils.boxAround(point.lat(), point.lng(), radiusMeters);
        var facilities = facilityRepository.findByBoundingBox(box.swLat(), box.swLng(), box.neLat(), box.neLng())
                .stream().filter(Facility::getUsedForRouting).toList();

        LocalProjection projection = new LocalProjection(point.lat(), point.lng());
        FacilitySpatialIndex index = new FacilitySpatialIndex(facilities, projection, geometryFactory);
        Geometry center = geometryFactory.createPoint(projection.toMeters(point.lat(), point.lng()));

        Map<FacilityType, Integer> counts = new EnumMap<>(FacilityType.class);
        Map<FacilityType, Double> typeScores = new EnumMap<>(FacilityType.class);
        for (FacilityType type : FacilityType.values()) {
            int count = index.count(center, type, radiusMeters);
            counts.put(type, count);
            typeScores.put(type, SafetyScoreCalculator.typeScore(type, count, 0, radiusMeters));
        }
        double score = SafetyScoreCalculator.weightedAverage(typeScores,
                presetWeightResolver.resolve(PresetWeightResolver.DEFAULT).weights());
        return new SafetyScoreResponse(Math.round(score * 1000) / 10.0,
                counts.get(FacilityType.CCTV), counts.get(FacilityType.STREETLIGHT));
    }
}
