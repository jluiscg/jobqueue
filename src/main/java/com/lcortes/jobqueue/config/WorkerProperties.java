package com.lcortes.jobqueue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jobqueue.worker")
public class WorkerProperties {

    private int poolSize = 10;
    private int pollingIntervalMs = 500;
    private int lockDurationMinutes = 5;

    public int getPoolSize() { return poolSize; }
    public void setPoolSize(int poolSize) { this.poolSize = poolSize; }

    public int getPollingIntervalMs() { return pollingIntervalMs; }
    public void setPollingIntervalMs(int pollingIntervalMs) { this.pollingIntervalMs = pollingIntervalMs; }

    public int getLockDurationMinutes() { return lockDurationMinutes; }
    public void setLockDurationMinutes(int lockDurationMinutes) { this.lockDurationMinutes = lockDurationMinutes; }
}