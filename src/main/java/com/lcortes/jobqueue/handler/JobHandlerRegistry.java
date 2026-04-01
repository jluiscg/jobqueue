package com.lcortes.jobqueue.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JobHandlerRegistry {

    private static final Logger log = LoggerFactory.getLogger(JobHandlerRegistry.class);

    //Map for strategy pattern
    private final Map<String, JobHandler> handlers;

    //Spring injects all implementations of job handlers
    public JobHandlerRegistry(List<JobHandler> availableHandlers) {
        // Convert the list into a fast, immutable Map grouped by the handler's type
        this.handlers = availableHandlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        JobHandler::getType,
                        handler -> handler,
                        (existing, duplicate) -> {
                            throw new IllegalStateException("Duplicate JobHandler type found: " + existing.getType());
                        }
                ));

        log.info("JobHandlerRegistry initialized with {} handlers: {}",
                handlers.size(), handlers.keySet());
    }

    /**
     * Retrieves the correct handler for a given job type.
     *
     * @param type The job type string (e.g., "HTTP_WEBHOOK").
     * @return An Optional containing the handler, or empty if not found.
     */
    public Optional<JobHandler> getHandler(String type) {
        return Optional.ofNullable(handlers.get(type));
    }
}