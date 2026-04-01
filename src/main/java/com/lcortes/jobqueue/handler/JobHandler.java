package com.lcortes.jobqueue.handler;

public interface JobHandler {

    /**
     * @return The unique string identifier for this job type (e.g., "SEND_EMAIL").
     */
    String getType();

    /**
     * Executes the job logic.
     * * @param context The immutable context of the current execution attempt.
     * @return A JobResult containing the output data to be saved.
     * @throws Exception Any unhandled exception will be caught by the engine,
     * triggering a failure and potential retry.
     */
    JobResult execute(JobContext context) throws Exception;
}