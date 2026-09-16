package com.Coming.Backend.policy.dto;

import com.Coming.Backend.policy.entity.PolicyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PolicyRegisterRequest(
        @NotNull
        PolicyType type,

        @NotBlank
        @Size(max = 50)
        String version,

        @NotNull
        LocalDate effectiveDate,

        @NotBlank
        String changeSummary,

        @NotBlank
        @Size(max = 500)
        String detailUrl,

        @NotNull
        Boolean requiresReconsent
) {
}
