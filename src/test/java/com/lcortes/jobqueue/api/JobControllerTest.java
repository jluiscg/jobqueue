package com.lcortes.jobqueue.api;

import com.lcortes.jobqueue.domain.JobStatus;
import com.lcortes.jobqueue.domain.exception.JobConflictException;
import com.lcortes.jobqueue.domain.exception.JobNotFoundException;
import com.lcortes.jobqueue.engine.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class JobControllerTest {

    private final JobService jobService = mock(JobService.class);
    private final JobController controller = new JobController(jobService);

    @Test
    void shouldReturn204WhenCancellationSucceeds() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response = controller.cancelJob(id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(jobService).cancelJob(id);
    }

    @Test
    void shouldThrowJobNotFoundExceptionWhenCancellingMissingJob() {
        UUID id = UUID.randomUUID();
        doThrow(new JobNotFoundException(id)).when(jobService).cancelJob(id);

        assertThrows(JobNotFoundException.class, () -> controller.cancelJob(id));
        verify(jobService).cancelJob(id);
    }

    @Test
    void shouldThrowJobConflictExceptionWhenJobCannotBeCancelled() {
        UUID id = UUID.randomUUID();
        doThrow(new JobConflictException(id, JobStatus.RUNNING.name())).when(jobService).cancelJob(id);

        assertThrows(JobConflictException.class, () -> controller.cancelJob(id));
        verify(jobService).cancelJob(id);
    }
}

