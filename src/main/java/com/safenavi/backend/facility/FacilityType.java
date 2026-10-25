package com.safenavi.backend.facility;

import java.util.Arrays;
import java.util.Optional;

/**
 * Facility.type codes plus the parameters of the radius-density safety model.
 * influenceRadiusMeters: how far from a path a facility still counts.
 * saturationPer100m: count within that radius along a 100m stretch that already scores 1.0.
 */
public enum FacilityType {

    CCTV("cctv", 50, 1.0),
    STREETLIGHT("streetlight", 30, 3.0),
    GUARDIAN_HOUSE("guardianHouse", 150, 1.0),
    POLICE("police", 500, 1.0),
    EMERGENCY_MEDICAL("emergencyMedical", 1000, 1.0);

    private final String code;
    private final double influenceRadiusMeters;
    private final double saturationPer100m;

    FacilityType(String code, double influenceRadiusMeters, double saturationPer100m) {
        this.code = code;
        this.influenceRadiusMeters = influenceRadiusMeters;
        this.saturationPer100m = saturationPer100m;
    }

    public static Optional<FacilityType> fromCode(String code) {
        return Arrays.stream(values()).filter(t -> t.code.equals(code)).findFirst();
    }

    public static double maxInfluenceRadius() {
        return Arrays.stream(values()).mapToDouble(FacilityType::getInfluenceRadiusMeters).max().orElse(0);
    }

    public String getCode() {
        return code;
    }

    public double getInfluenceRadiusMeters() {
        return influenceRadiusMeters;
    }

    public double getSaturationPer100m() {
        return saturationPer100m;
    }
}
