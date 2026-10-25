package com.safenavi.backend.route;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.facility.FacilityType;

/** Fixed route presets: which facility types count toward safety, their weights, and alpha. */
@Component
public class PresetWeightResolver {

    public static final String DEFAULT = "default";

    /** alpha: how strongly danger_penalty inflates a segment's distance in edge_cost. */
    public record Preset(String name, Map<FacilityType, Double> weights, double alpha) {
    }

    private final Map<String, Preset> presets = new LinkedHashMap<>();

    public PresetWeightResolver() {
        register("cctv", 0.5, Map.of(FacilityType.CCTV, 1.0));
        register("streetlight", 0.5, Map.of(FacilityType.STREETLIGHT, 1.0));
        register("guardianHouse", 0.5, Map.of(FacilityType.GUARDIAN_HOUSE, 1.0));
        register("publicSafety", 0.5, Map.of(FacilityType.POLICE, 0.5, FacilityType.EMERGENCY_MEDICAL, 0.5));
        register(DEFAULT, 0.5, Map.of(
                FacilityType.STREETLIGHT, 0.25,
                FacilityType.CCTV, 0.25,
                FacilityType.GUARDIAN_HOUSE, 0.25,
                FacilityType.POLICE, 0.125,
                FacilityType.EMERGENCY_MEDICAL, 0.125));
    }

    public Preset resolve(String name) {
        Preset preset = presets.get(name);
        if (preset == null) {
            throw new BusinessException(ErrorCode.UNKNOWN_PRESET, "지원하지 않는 프리셋: " + name + " (가능: " + presets.keySet() + ")");
        }
        return preset;
    }

    public List<Preset> all() {
        return List.copyOf(presets.values());
    }

    private void register(String name, double alpha, Map<FacilityType, Double> weights) {
        presets.put(name, new Preset(name, new EnumMap<>(weights), alpha));
    }
}
