package com.lcortes.jobqueue.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkerPropertiesTest {

    @Test
    void shouldExposeExpectedDefaultValues() {
        WorkerProperties properties = new WorkerProperties();

        assertEquals(10, properties.getPoolSize());
        assertEquals(500, properties.getPollingIntervalMs());
        assertEquals(5, properties.getLockDurationMinutes());
        assertEquals(1000, properties.getErrorBackoffMs());
    }

    @Test
    void shouldBindConfiguredValuesFromPropertySource() {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("test", Map.of(
                "jobqueue.worker.pool-size", 24,
                "jobqueue.worker.polling-interval-ms", 125,
                "jobqueue.worker.lock-duration-minutes", 9,
                "jobqueue.worker.error-backoff-ms", 2500
        )));

        Binder binder = new Binder(ConfigurationPropertySources.from(propertySources));
        WorkerProperties properties = binder.bind("jobqueue.worker", Bindable.of(WorkerProperties.class))
                .orElseThrow(IllegalStateException::new);

        assertEquals(24, properties.getPoolSize());
        assertEquals(125, properties.getPollingIntervalMs());
        assertEquals(9, properties.getLockDurationMinutes());
        assertEquals(2500, properties.getErrorBackoffMs());
    }
}


