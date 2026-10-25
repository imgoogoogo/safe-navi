package com.safenavi.backend.facility;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safenavi.backend.common.geo.BoundingBox;
import com.safenavi.backend.common.response.ApiResponse;

@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping
    public ApiResponse<List<FacilityResponse>> list(@RequestParam double swLat, @RequestParam double swLng,
            @RequestParam double neLat, @RequestParam double neLng, @RequestParam(required = false) String type) {
        return ApiResponse.ok(facilityService.findInBox(new BoundingBox(swLat, swLng, neLat, neLng), type));
    }
}
