package com.safenavi.backend.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** KEEP clears the flags (report leaves the review queue); REMOVED soft-deletes the report. */
public record ReportStatusChangeRequest(@NotNull @Pattern(regexp = "KEEP|REMOVED") String status) {
}
