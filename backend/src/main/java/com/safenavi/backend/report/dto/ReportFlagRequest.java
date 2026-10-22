package com.safenavi.backend.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportFlagRequest(@NotBlank @Size(max = 255) String reason) {
}
