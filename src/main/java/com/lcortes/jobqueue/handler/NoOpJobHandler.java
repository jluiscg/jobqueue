package com.lcortes.jobqueue.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoOpJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(NoOpJobHandler.class);
    @Override
    public String getType() {
        return "NO_OP";
    }

    @Override
    public JobResult execute(JobContext context) throws Exception {
        log.info("Executing NO_OP job [ID: {}]. Attempt: {}", context.jobId(), context.attemptNumber());

        //send back the context payload as a result of no op
        return new JobResult(context.payload());
    }

}
