package com.lcortes.jobqueue.engine;

import com.lcortes.jobqueue.config.WorkerProperties;
import com.lcortes.jobqueue.domain.Job;
import com.lcortes.jobqueue.handler.JobContext;
import com.lcortes.jobqueue.handler.JobHandler;
import com.lcortes.jobqueue.handler.JobHandlerRegistry;
import com.lcortes.jobqueue.handler.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Worker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Worker.class);

    private final String workerId;
    private final WorkerProperties properties;
    private final JobService jobService;
    private final JobHandlerRegistry registry;

    // volatile ensures thread-safe visibility when stop() is called by the shutdown hook
    private volatile boolean running = true;

    public Worker(String workerId, WorkerProperties properties, JobService jobService, JobHandlerRegistry registry) {
        this.workerId = workerId;
        this.properties = properties;
        this.jobService = jobService;
        this.registry = registry;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        log.info("Worker [{}] started.", workerId);

        while (running) {
            try {
                // 1. Attempt to claim a job (transactional boundary inside JobService)
                Optional<Job> claimedJob = jobService.claimNextJob(workerId);

                if (claimedJob.isPresent()) {
                    // 2. Process it
                    processJob(claimedJob.get());
                    // 3. GREEDY POLLING: We don't sleep. We immediately loop to grab the next job.
                } else {
                    // 4. No jobs available. Sleep to avoid burning CPU cycles (Fallback Polling).
                    Thread.sleep(properties.getPollingIntervalMs());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Worker [{}] interrupted, shutting down.", workerId);
                running = false;
            } catch (Exception e) {
                log.error("Worker [{}] encountered an unexpected engine error", workerId, e);
                sleepQuietly(properties.getErrorBackoffMs()); // Prevent tight-looping if the DB connection drops
            }
        }
        log.info("Worker [{}] stopped.", workerId);
    }

    private void processJob(Job job) {
        long startTime = System.currentTimeMillis();
        try {
            Optional<JobHandler> handlerOpt = registry.getHandler(job.getType());

            if (handlerOpt.isEmpty()) {
                throw new IllegalStateException("No handler registered for job type: " + job.getType());
            }

            // Execute the handler logic
            JobContext context = new JobContext(job.getId(), job.getType(), job.getPayload(), job.getAttemptCount());
            JobResult result = handlerOpt.get().execute(context);

            // Mark successful completion (centralized logging!)
            long duration = System.currentTimeMillis() - startTime;
            jobService.markCompleted(job.getId(), result);
            log.info("Job [{}] COMPLETED in {}ms", job.getId(), duration);

        } catch (Exception e) {
            // Mark failure (JobService will handle retries vs dead-letter queue in Phase 2)
            long duration = System.currentTimeMillis() - startTime;
            jobService.markFailed(job.getId(), e);
            log.error("Job [{}] FAILED after {}ms. Reason: {}", job.getId(), duration, e.getMessage());
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}