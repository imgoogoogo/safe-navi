package com.safenavi.backend.report.dto;

import jakarta.validation.constraints.Size;

/** Every field is optional; null means "leave unchanged". lat and lng must be sent together. */
public record ReportUpdateRequest(
        String type,
        Double lat,
        Double lng,
        @Size(max = 500) String memo) {
}
