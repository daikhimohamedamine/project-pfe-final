package com.digitalassethub.medical.api.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiRecommendRequest(
        @NotNull Long employeeId,
        @NotBlank String symptoms
) {}
