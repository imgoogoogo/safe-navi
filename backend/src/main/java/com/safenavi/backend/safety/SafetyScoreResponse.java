package com.safenavi.backend.safety;

/** safetyScore is 0-100, the default-preset weighted density score around the point. */
public record SafetyScoreResponse(double safetyScore, int cctvCount, int streetlightCount) {
}
