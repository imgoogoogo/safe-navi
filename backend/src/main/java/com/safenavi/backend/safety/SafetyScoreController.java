package com.safenavi.backend.safety;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safenavi.backend.common.geo.Coordinate;
import com.safenavi.backend.common.response.ApiResponse;

@RestController
public class SafetyScoreController {

    private final SafetyScoreService safetyScoreService;

    public SafetyScoreController(SafetyScoreService safetyScoreService) {
        this.safetyScoreService = safetyScoreService;
    }

    @GetMapping("/api/safety-score")
    public ApiResponse<SafetyScoreResponse> score(@RequestParam double lat, @RequestParam double lng,
            @RequestParam(required = false) Double radius) {
        double r = radius == null ? SafetyScoreService.DEFAULT_RADIUS_METERS : radius;
        return ApiResponse.ok(safetyScoreService.score(new Coordinate(lat, lng), r));
    }
}
