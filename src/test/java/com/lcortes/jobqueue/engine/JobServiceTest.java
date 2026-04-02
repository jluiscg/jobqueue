package com.lcortes.jobqueue.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcortes.jobqueue.config.WorkerProperties;
import com.lcortes.jobqueue.domain.Job;
import com.lcortes.jobqueue.domain.JobStatus;
import com.lcortes.jobqueue.handler.JobResult;
import com.lcortes.jobqueue.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobServiceTest {

    private final JobRepository jobRepository = mock(JobRepository.class);
    private final WorkerProperties properties = mock(WorkerProperties.class);
    private final JobService jobService = new JobService(jobRepository, properties);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldSubmitJobsAsQueuedBeforePersisting() {
        Job job = new Job();
        job.setStatus(JobStatus.COMPLETED);

        Job savedJob = jobService.submitJob(job);

        assertSame(job, savedJob);
        assertEquals(JobStatus.QUEUED, job.getStatus());
        verify(jobRepository).save(job);
    }

    @Test
    void shouldDelegateClaimingToRepositoryWithConfiguredLockDuration() {
        when(properties.getLockDurationMinutes()).thenReturn(7);
        Job claimedJob = new Job();
        when(jobRepository.claimNextJob("worker-1", 7)).thenReturn(Optional.of(claimedJob));

        Optional<Job> result = jobService.claimNextJob("worker-1");

        assertTrue(result.isPresent());
        assertSame(claimedJob, result.orElseThrow());
        verify(jobRepository).claimNextJob("worker-1", 7);
    }

    @Test
    void shouldMarkJobCompletedAndReleaseLock() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job();
        job.setLockedBy("worker-1");
        job.setLockExpiresAt(Instant.now().plusSeconds(60));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        JsonNode output = objectMapper.createObjectNode().put("status", "ok");

        jobService.markCompleted(jobId, new JobResult(output));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());

        Job savedJob = captor.getValue();
        assertSame(job, savedJob);
        assertEquals(JobStatus.COMPLETED, savedJob.getStatus());
        assertSame(output, savedJob.getResult());
        assertNotNull(savedJob.getCompletedAt());
        assertFalse(savedJob.getCompletedAt().isAfter(Instant.now().plusSeconds(1)));
        assertNull(savedJob.getLockedBy());
        assertNull(savedJob.getLockExpiresAt());
    }

    @Test
    void shouldMarkJobRetryingWhenAttemptsRemain() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job();
        job.setAttemptCount(1);
        job.setMaxRetries(3);
        job.setLockedBy("worker-1");
        job.setLockExpiresAt(Instant.now().plusSeconds(60));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        Instant before = Instant.now();
        jobService.markFailed(jobId, new RuntimeException("boom"));
        Instant after = Instant.now();

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());

        Job savedJob = captor.getValue();
        assertSame(job, savedJob);
        assertEquals(JobStatus.RETRYING, savedJob.getStatus());
        assertNull(savedJob.getLockedBy());
        assertNull(savedJob.getLockExpiresAt());
        assertNotNull(savedJob.getRunAt());

        long delaySeconds = Duration.between(before, savedJob.getRunAt()).toSeconds();
        long minDelay = 19L;
        long maxDelay = 21L;
        assertTrue(delaySeconds >= minDelay && delaySeconds <= maxDelay,
                "Expected retry delay close to 20 seconds but was " + delaySeconds + " seconds");
        assertFalse(savedJob.getRunAt().isAfter(after.plusSeconds(21)));
    }

    @Test
    void shouldMoveJobToDeadWhenRetriesAreExhausted() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job();
        job.setAttemptCount(3);
        job.setMaxRetries(3);
        job.setLockedBy("worker-1");
        job.setLockExpiresAt(Instant.now().plusSeconds(60));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        jobService.markFailed(jobId, new RuntimeException("boom"));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());

        Job savedJob = captor.getValue();
        assertSame(job, savedJob);
        assertEquals(JobStatus.DEAD, savedJob.getStatus());
        assertNull(savedJob.getLockedBy());
        assertNull(savedJob.getLockExpiresAt());
        assertNull(savedJob.getRunAt());
    }
}


