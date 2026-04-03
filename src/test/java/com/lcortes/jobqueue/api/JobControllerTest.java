package com.lcortes.jobqueue.api;

import com.lcortes.jobqueue.domain.Job;
import com.lcortes.jobqueue.engine.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobControllerTest {

    private final JobService jobService = mock(JobService.class);
    private final JobController controller = new JobController(jobService);

    @Test
    void shouldReturn404WhenCancellingMissingJob() {
        UUID id = UUID.randomUUID();
        when(jobService.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = controller.cancelJob(id);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(jobService, never()).cancelJob(id);
    }

    @Test
    void shouldReturn204WhenCancellationSucceeds() {
        UUID id = UUID.randomUUID();
        when(jobService.findById(id)).thenReturn(Optional.of(new Job()));
        when(jobService.cancelJob(id)).thenReturn(true);

        ResponseEntity<Void> response = controller.cancelJob(id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void shouldReturn409WhenJobExistsButCannotBeCancelled() {
        UUID id = UUID.randomUUID();
        when(jobService.findById(id)).thenReturn(Optional.of(new Job()));
        when(jobService.cancelJob(id)).thenReturn(false);

        ResponseEntity<Void> response = controller.cancelJob(id);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}

