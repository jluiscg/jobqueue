package com.lcortes.jobqueue.engine;

import com.lcortes.jobqueue.config.WorkerProperties;
import com.lcortes.jobqueue.handler.JobHandlerRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class WorkerPool {

    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);

    private final WorkerProperties properties;
    private final JobService jobService;
    private final JobHandlerRegistry registry;

    private final List<Worker> workers = new ArrayList<>();
    private final String instanceId;
    private ExecutorService executorService;

    public WorkerPool(WorkerProperties properties, JobService jobService, JobHandlerRegistry registry) {
        this.properties = properties;
        this.jobService = jobService;
        this.registry = registry;
        this.instanceId = generateInstanceId();
    }

    @PostConstruct
    public void start() {
        int poolSize = properties.getPoolSize();
        log.info("Starting WorkerPool with {} workers. Instance ID: {}", poolSize, instanceId);

        // Java 21 Feature: Virtual Threads
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < poolSize; i++) {
            // Generates IDs like "worker-macbook-abc1234-thread-0"
            String workerId = instanceId + "-thread-" + i;
            Worker worker = new Worker(workerId, properties, jobService, registry);

            workers.add(worker);
            executorService.submit(worker);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down WorkerPool gracefully...");

        // 1. Tell all workers to finish their current job and stop polling
        workers.forEach(Worker::stop);

        // 2. Shut down the executor
        executorService.shutdown();
        try {
            // Give jobs 10 seconds to finish currently executing logic
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Timeout reached. Forcing WorkerPool shutdown...");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("WorkerPool shutdown complete.");
    }

    /**
     * Generates an instance-unique workerId as specified in AGENTS.md
     */
    private String generateInstanceId() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String shortUuid = UUID.randomUUID().toString().substring(0, 8);
            return "worker-" + hostname + "-" + shortUuid;
        } catch (UnknownHostException e) {
            return "worker-unknown-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}