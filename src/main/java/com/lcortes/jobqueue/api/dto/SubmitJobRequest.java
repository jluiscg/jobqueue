package com.lcortes.jobqueue.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record SubmitJobRequest(
        @NotBlank(message = "type is required")
        String type,

        @NotNull(message = "payload is required")
        JsonNode payload,

        String priority, // e.g., "HIGH", "NORMAL". Defaults to "NORMAL" if null.

        @Min(value = 0, message = "Max retries should be 0 - 10") @Max(value = 10, message = "Max retries should be 0 - 10")
        Integer maxRetries, // Defaults to 3

        Instant runAt
) {}
