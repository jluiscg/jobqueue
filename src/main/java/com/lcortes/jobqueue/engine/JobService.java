package com.lcortes.jobqueue.engine;

import com.lcortes.jobqueue.api.dto.SubmitJobRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.lcortes.jobqueue.config.WorkerProperties;
import com.lcortes.jobqueue.domain.Job;
import com.lcortes.jobqueue.domain.JobStatus;
import com.lcortes.jobqueue.domain.exception.JobConflictException;
import com.lcortes.jobqueue.domain.exception.JobNotFoundException;
import com.lcortes.jobqueue.handler.JobResult;
import com.lcortes.jobqueue.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final WorkerProperties properties;
    private final ObjectMapper objectMapper;

    // Strict constructor injection
    public JobService(JobRepository jobRepository, WorkerProperties properties, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Called by the Worker to grab the next job.
     */
    @Transactional
    public Optional<Job> claimNextJob(String workerId) {
        return jobRepository.claimNextJob(workerId, properties.getLockDurationMinutes());
    }

    /**
     * Called by the Worker when a JobHandler returns a successful JobResult.
     */
    @Transactional
    public void markCompleted(UUID jobId, JobResult result) {
        jobRepository.findById(jobId).ifPresentOrElse(job -> {
            job.setStatus(JobStatus.COMPLETED);
            job.setResult(result.output()); // hypersistence-utils handles the JsonNode -> JSONB mapping
            job.setCompletedAt(OffsetDateTime.now().toInstant());

            // Release the lock
            job.setLockedBy(null);
            job.setLockExpiresAt(null);

            jobRepository.save(job);
        }, () -> log.warn("Worker attempted to complete a job that no longer exists: {}", jobId));
    }

    /**
     * Called by the Worker when a JobHandler throws an Exception.
     * Implements the State Machine and Exponential Backoff from Phase 2.
     */
    @Transactional
    public void markFailed(UUID jobId, Exception exception) { //exception will be used later
        jobRepository.findById(jobId).ifPresentOrElse(job -> {
            // Release the lock immediately so it doesn't block
            job.setLockedBy(null);
            job.setLockExpiresAt(null);

            if (job.getAttemptCount() >= job.getMaxRetries()) {
                // Exhausted retries -> DEAD
                job.setStatus(JobStatus.DEAD);
                log.error("Job [{}] moved to DEAD. Max retries ({}) exhausted.",
                        job.getId(), job.getMaxRetries());
            } else {
                // Has retries remaining -> RETRYING
                job.setStatus(JobStatus.RETRYING);

                // Exponential Backoff Formula: min(2^attempt * 10, 3600)
                long delaySeconds = Math.min((long) Math.pow(2, job.getAttemptCount()) * 10, 3600);
                job.setRunAt(OffsetDateTime.now().plusSeconds(delaySeconds).toInstant());

                log.warn("Job [{}] failed. Retrying in {} seconds (Attempt {}/{})",
                        job.getId(), delaySeconds, job.getAttemptCount(), job.getMaxRetries());
            }

            jobRepository.save(job);
        }, () -> log.warn("Worker attempted to fail a job that no longer exists: {}", jobId));
    }

    /**
     * Called by the REST Controller (Task 1.10) to submit a new job to the queue.
     */
    @Transactional
    public Job submitJob(SubmitJobRequest jobRequest) {
        Job job = new Job();
        job.setType(jobRequest.type());
        try {
            // Bridge API JsonNode (tools.jackson) to persistence JsonNode (com.fasterxml).
            job.setPayload(objectMapper.readTree(jobRequest.payload().toString()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid job payload JSON", e);
        }

        // Map values with safe defaults
        job.setPriority(jobRequest.priority() != null ?
                com.lcortes.jobqueue.domain.JobPriority.valueOf(jobRequest.priority().toUpperCase()).getValue() :
                com.lcortes.jobqueue.domain.JobPriority.NORMAL.getValue());

        if (jobRequest.maxRetries() != null) job.setMaxRetries(jobRequest.maxRetries());
        if (jobRequest.runAt() != null) job.setRunAt(jobRequest.runAt());

        job.setStatus(JobStatus.QUEUED);
        return jobRepository.save(job);
    }

    /**
     * Retrieve a job by id.
     * @throws JobNotFoundException if the job does not exist
     */
    @Transactional(readOnly = true)
    public Job getJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }

    /**
     * Fetches a paginated list of jobs, optionally filtered by status and type.
     */
    @Transactional(readOnly = true)
    public Page<Job> listJobs(JobStatus status, String type, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jobRepository.findWithFilters(status, type, pageRequest);
    }

    /**
     * Attempts to cancel a job.
     * Per architectural rules, only jobs that have not been claimed can be cancelled.
     * @throws JobNotFoundException if the job does not exist
     * @throws JobConflictException if the job is in a non-cancellable state
     */
    @Transactional
    public void cancelJob(UUID id) {
        Optional<Job> jobOpt = jobRepository.findById(id);

        if (jobOpt.isEmpty()) {
            throw new JobNotFoundException(id);
        }

        Job job = jobOpt.get();

        if (job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RETRYING) {
            job.setStatus(JobStatus.CANCELLED);
            jobRepository.save(job);
            log.info("Job [{}] was successfully CANCELLED via API", id);
        } else {
            log.warn("Attempted to cancel Job [{}] but it is currently in state {}", id, job.getStatus());
            throw new JobConflictException(id, job.getStatus().name());
        }
    }
}