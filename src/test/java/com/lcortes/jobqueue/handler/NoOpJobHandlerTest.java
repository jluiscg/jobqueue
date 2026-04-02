package com.lcortes.jobqueue.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;

class NoOpJobHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnTheOriginalPayloadUnchanged() throws Exception {
        NoOpJobHandler handler = new NoOpJobHandler();
        JobContext context = new JobContext(
                UUID.randomUUID(),
                handler.getType(),
                objectMapper.readTree("{\"message\":\"hello\"}"),
                2
        );

        JobResult result = handler.execute(context);

        assertSame(context.payload(), result.output());
    }
}

