package com.lcortes.jobqueue.api;

import com.lcortes.jobqueue.api.dto.JobResponse;
import com.lcortes.jobqueue.api.dto.PagedResponse;
import com.lcortes.jobqueue.api.dto.SubmitJobRequest;
import com.lcortes.jobqueue.domain.Job;
import com.lcortes.jobqueue.domain.JobStatus;
import com.lcortes.jobqueue.engine.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody SubmitJobRequest jobRequest) {
        Job savedJob = jobService.submitJob(jobRequest);

        // 202 Accepted signals that the request was received and queued for async processing
        return ResponseEntity.accepted().body(JobResponse.fromEntity(savedJob));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
        Optional<Job> job = jobService.findById(id);

        return job.map(j -> ResponseEntity.ok(JobResponse.fromEntity(j)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<PagedResponse<JobResponse>> listJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // We sort by created_at descending so the newest jobs appear first in the dashboard
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Job> jobPage = jobService.listJobs(status, type, pageRequest);

        List<JobResponse> content = jobPage.getContent().stream()
                .map(JobResponse::fromEntity)
                .toList();

        PagedResponse<JobResponse> response = new PagedResponse<>(
                content,
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.isLast()
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelJob(@PathVariable UUID id) {
        if (jobService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean cancelled = jobService.cancelJob(id);

        if (cancelled) {
            return ResponseEntity.noContent().build(); // 204 No Content, successfully canceled
        } else {
            // 409 Conflict if the job is already running or otherwise not cancellable
            return ResponseEntity.status(409).build();
        }
    }
}