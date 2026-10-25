package com.safenavi.backend.route;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safenavi.backend.common.geo.Coordinate;
import com.safenavi.backend.common.response.ApiResponse;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public ApiResponse<RouteDetail> route(@RequestParam double originLat, @RequestParam double originLng,
            @RequestParam double destLat, @RequestParam double destLng,
            @RequestParam(defaultValue = PresetWeightResolver.DEFAULT) String preset) {
        return ApiResponse.ok(routeService.findRoute(new Coordinate(originLat, originLng),
                new Coordinate(destLat, destLng), preset));
    }

    @GetMapping("/compare")
    public ApiResponse<List<RouteSummary>> compare(@RequestParam double originLat, @RequestParam double originLng,
            @RequestParam double destLat, @RequestParam double destLng) {
        return ApiResponse.ok(routeService.compareRoutes(new Coordinate(originLat, originLng),
                new Coordinate(destLat, destLng)));
    }
}
