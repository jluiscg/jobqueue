package com.lcortes.jobqueue.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcortes.jobqueue.api.dto.SubmitJobRequest;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final tools.jackson.databind.ObjectMapper toolsObjectMapper = new tools.jackson.databind.ObjectMapper();
    private final JobService jobService = new JobService(jobRepository, properties, objectMapper);

    @BeforeEach
    void setUp() {
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldSubmitJobsAsQueuedBeforePersisting() throws Exception {
        tools.jackson.databind.JsonNode payload = toolsObjectMapper.createObjectNode().put("key", "value");
        SubmitJobRequest request = new SubmitJobRequest("test-type", payload, "HIGH", 5, null);
        Job savedJob = jobService.submitJob(request);

        assertNotNull(savedJob);
        assertEquals(JobStatus.QUEUED, savedJob.getStatus());
        assertEquals("test-type", savedJob.getType());
        assertEquals(com.lcortes.jobqueue.domain.JobPriority.HIGH.getValue(), savedJob.getPriority());
        assertEquals(5, savedJob.getMaxRetries());
        assertEquals(objectMapper.readTree(payload.toString()), savedJob.getPayload());

        verify(jobRepository).save(any(Job.class));
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
        jobService.markFailed(jobId);
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

        jobService.markFailed(jobId);

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
