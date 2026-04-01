package com.lcortes.jobqueue.domain;

public enum JobStatus {
    QUEUED,
    RUNNING,
    RETRYING,
    COMPLETED,
    DEAD,
    CANCELLED
}