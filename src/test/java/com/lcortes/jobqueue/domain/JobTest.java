package com.lcortes.jobqueue.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JobTest {

    @Test
    void shouldInitializeWithExpectedDefaults() {
        Job job = new Job();

        assertNull(job.getId());
        assertNull(job.getType());
        assertEquals(JobStatus.QUEUED, job.getStatus());
        assertEquals(JobPriority.NORMAL.getValue(), job.getPriority());
        assertEquals(0, job.getAttemptCount());
        assertEquals(3, job.getMaxRetries());
        assertNull(job.getRunAt());
        assertNull(job.getStartedAt());
        assertNull(job.getCompletedAt());
        assertNull(job.getLockedBy());
        assertNull(job.getLockExpiresAt());
        assertNull(job.getCreatedAt());
        assertNull(job.getUpdatedAt());
    }

    @Test
    void shouldTranslatePriorityEnumToAndFromPrimitiveValue() {
        Job job = new Job();

        job.setPriorityEnum(JobPriority.CRITICAL);

        assertEquals(JobPriority.CRITICAL.getValue(), job.getPriority());
        assertEquals(JobPriority.CRITICAL, job.getPriorityEnum());
    }

    @Test
    void shouldIncrementAttemptCount() {
        Job job = new Job();

        job.incrementAttemptCount();
        job.incrementAttemptCount();

        assertEquals(2, job.getAttemptCount());
    }

    @Test
    void shouldPopulateAuditTimestampsOnCreateAndUpdate() {
        Job job = new Job();

        job.onCreate();
        Instant createdAt = job.getCreatedAt();
        Instant updatedAt = job.getUpdatedAt();

        assertNotNull(createdAt);
        assertNotNull(updatedAt);
        assertFalse(updatedAt.isBefore(createdAt));

        job.onUpdate();

        assertNotNull(job.getUpdatedAt());
        assertFalse(job.getUpdatedAt().isBefore(updatedAt));
    }
}

