package com.safenavi.backend.geo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safenavi.backend.common.geo.Coordinate;
import com.safenavi.backend.common.response.ApiResponse;

@RestController
@RequestMapping("/api/geo")
public class GeoController {

    private final GeoService geoService;

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    @GetMapping("/reverse")
    public ApiResponse<ReverseGeocodeResponse> reverse(@RequestParam double lat, @RequestParam double lng) {
        return ApiResponse.ok(geoService.reverse(new Coordinate(lat, lng)));
    }
}
