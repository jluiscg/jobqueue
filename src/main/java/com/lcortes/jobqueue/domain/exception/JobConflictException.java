package com.lcortes.jobqueue.domain.exception;

import java.util.UUID;

public class JobConflictException extends RuntimeException {

    public JobConflictException(UUID jobId, String currentStatus) {
        super("Cannot cancel job " + jobId + ": current status is " + currentStatus);
    }
}
