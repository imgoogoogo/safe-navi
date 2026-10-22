package com.safenavi.backend.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** multipart/form-data fields; the optional photo arrives as a separate "image" part. */
public record ReportCreateRequest(
        @NotBlank String type,
        @NotNull Double lat,
        @NotNull Double lng,
        @Size(max = 500) String memo) {
}
