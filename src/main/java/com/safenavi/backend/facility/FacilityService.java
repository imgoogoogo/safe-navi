package com.safenavi.backend.facility;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.common.geo.BoundingBox;

@Service
public class FacilityService {

    private final FacilityRepository facilityRepository;

    public FacilityService(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @Transactional(readOnly = true)
    public List<FacilityResponse> findInBox(BoundingBox box, String type) {
        box.requireMaxSpan(BoundingBox.MAX_SPAN_DEGREES);
        List<Facility> facilities;
        if (type == null || type.isBlank()) {
            facilities = facilityRepository.findByBoundingBox(box.swLat(), box.swLng(), box.neLat(), box.neLng());
        } else {
            FacilityType.fromCode(type)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "알 수 없는 시설 유형: " + type));
            facilities = facilityRepository.findByBoundingBoxAndType(box.swLat(), box.swLng(), box.neLat(),
                    box.neLng(), type);
        }
        return facilities.stream().map(FacilityResponse::from).toList();
    }
}
