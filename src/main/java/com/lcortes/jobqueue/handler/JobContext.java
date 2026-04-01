package com.lcortes.jobqueue.handler;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record JobContext(
        UUID jobId,
        String type,
        JsonNode payload,
        int attemptNumber
) {}