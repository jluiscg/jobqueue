package com.lcortes.jobqueue.engine;

import com.lcortes.jobqueue.config.WorkerProperties;
import com.lcortes.jobqueue.domain.Job;
import com.lcortes.jobqueue.domain.JobStatus;
import com.lcortes.jobqueue.handler.JobResult;
import com.lcortes.jobqueue.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    // Strict constructor injection
    public JobService(JobRepository jobRepository, WorkerProperties properties) {
        this.jobRepository = jobRepository;
        this.properties = properties;
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
    public void markFailed(UUID jobId, Exception exception) {
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
    public Job submitJob(Job job) {
        job.setStatus(JobStatus.QUEUED);
        return jobRepository.save(job);
    }
}