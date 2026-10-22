package com.safenavi.backend.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserStatusChangeRequest(@NotNull @Pattern(regexp = "ACTIVE|SUSPENDED") String status) {
}
