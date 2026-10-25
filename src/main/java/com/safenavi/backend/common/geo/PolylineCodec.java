package com.safenavi.backend.common.geo;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * RoadSegment.geometry is stored as a JSON array of [lat, lng] pairs in nodeA -> nodeB order.
 */
public final class PolylineCodec {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final TypeReference<List<double[]>> TYPE = new TypeReference<>() {
    };

    private PolylineCodec() {
    }

    public static String encode(List<double[]> points) {
        return MAPPER.writeValueAsString(points);
    }

    public static List<double[]> decode(String json) {
        return new ArrayList<>(MAPPER.readValue(json, TYPE));
    }
}
