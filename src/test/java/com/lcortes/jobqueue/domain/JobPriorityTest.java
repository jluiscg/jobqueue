package com.lcortes.jobqueue.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobPriorityTest {

    @Test
    void shouldResolveEnumFromStoredIntegerValue() {
        assertEquals(JobPriority.LOW, JobPriority.fromValue(0));
        assertEquals(JobPriority.NORMAL, JobPriority.fromValue(5));
        assertEquals(JobPriority.HIGH, JobPriority.fromValue(10));
        assertEquals(JobPriority.CRITICAL, JobPriority.fromValue(20));
    }

    @Test
    void shouldRejectUnknownIntegerValue() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> JobPriority.fromValue(42));

        assertTrue(exception.getMessage().contains("42"));
    }
}

