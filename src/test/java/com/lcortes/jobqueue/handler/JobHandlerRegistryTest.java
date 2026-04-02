package com.lcortes.jobqueue.handler;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobHandlerRegistryTest {

    @Test
    void shouldResolveHandlerByType() {
        JobHandler expectedHandler = new TestJobHandler("TYPE_A");
        JobHandlerRegistry registry = new JobHandlerRegistry(List.of(expectedHandler));

        Optional<JobHandler> handler = registry.getHandler("TYPE_A");

        assertTrue(handler.isPresent());
        assertEquals(expectedHandler, handler.orElseThrow());
    }

    @Test
    void shouldReturnEmptyWhenHandlerTypeIsUnknown() {
        JobHandlerRegistry registry = new JobHandlerRegistry(List.of(new TestJobHandler("TYPE_A")));

        assertTrue(registry.getHandler("MISSING").isEmpty());
    }

    @Test
    void shouldRejectDuplicateHandlerTypes() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                new JobHandlerRegistry(List.of(
                        new TestJobHandler("DUPLICATE"),
                        new TestJobHandler("DUPLICATE")
                )));

        assertTrue(exception.getMessage().contains("DUPLICATE"));
    }

    private static final class TestJobHandler implements JobHandler {

        private final String type;

        private TestJobHandler(String type) {
            this.type = type;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public JobResult execute(JobContext context) {
            return new JobResult(context.payload());
        }
    }
}

