package com.safenavi.backend.route;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.facility.FacilityType;

/** Fixed route presets: which facility types count toward safety and their weights. */
@Component
public class PresetWeightResolver {

    public static final String DEFAULT = "default";

    public record Preset(String name, Map<FacilityType, Double> weights) {
    }

    private final Map<String, Preset> presets = new LinkedHashMap<>();

    public PresetWeightResolver() {
        register("cctv", Map.of(FacilityType.CCTV, 1.0));
        register("streetlight", Map.of(FacilityType.STREETLIGHT, 1.0));
        register("guardianHouse", Map.of(FacilityType.GUARDIAN_HOUSE, 1.0));
        register("publicSafety", Map.of(FacilityType.POLICE, 0.5, FacilityType.EMERGENCY_MEDICAL, 0.5));
        register(DEFAULT, Map.of(
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

    private void register(String name, Map<FacilityType, Double> weights) {
        presets.put(name, new Preset(name, new EnumMap<>(weights)));
    }
}
