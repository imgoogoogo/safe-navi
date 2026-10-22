package com.safenavi.backend.graphbuild;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @param regionCode 시군구 code the built nodes/segments are tagged with; that region's old graph is replaced
 * @param pbfPath    OSM .pbf file on the server; defaults to graph-build.pbf-path
 * @param swLat      optional clip box (all four or none), e.g. to cut one 시군구 out of a national extract
 */
public record GraphBuildRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]{1,10}") String regionCode,
        String pbfPath,
        Double swLat, Double swLng, Double neLat, Double neLng) {
}
