package com.lcortes.jobqueue.api.dto;

import com.lcortes.jobqueue.domain.Job;
import com.lcortes.jobqueue.domain.JobStatus;
import com.lcortes.jobqueue.domain.JobPriority;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String type,
        JobStatus status,
        String priority,
        JsonNode payload,
        JsonNode result,
        int attemptCount,
        int maxRetries,
        Instant runAt,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // A clean static factory method to map from the Entity to the DTO
    public static JobResponse fromEntity(Job job) {
        return new JobResponse(
                job.getId(),
                job.getType(),
                job.getStatus(),
                JobPriority.fromValue(job.getPriority()).displayName(),
                toToolsJsonNode(job.getPayload()),
                toToolsJsonNode(job.getResult()),
                job.getAttemptCount(),
                job.getMaxRetries(),
                job.getRunAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }

    private static JsonNode toToolsJsonNode(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }

        return OBJECT_MAPPER.readTree(jsonNode.toString());
    }
}