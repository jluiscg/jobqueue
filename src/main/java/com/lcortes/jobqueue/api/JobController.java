package com.lcortes.jobqueue.api;

import com.lcortes.jobqueue.api.dto.JobResponse;
import com.lcortes.jobqueue.api.dto.PagedResponse;
import com.lcortes.jobqueue.api.dto.SubmitJobRequest;
import com.lcortes.jobqueue.domain.Job;
import com.lcortes.jobqueue.domain.JobStatus;
import com.lcortes.jobqueue.engine.JobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody SubmitJobRequest jobRequest) {
        Job savedJob = jobService.submitJob(jobRequest);

        return ResponseEntity.created(java.net.URI.create("/api/jobs/" + savedJob.getId()))
                .body(JobResponse.fromEntity(savedJob));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
        Job job = jobService.getJob(id);
        return ResponseEntity.ok(JobResponse.fromEntity(job));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<JobResponse>> listJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(0) @Max(100) int size
    ) {
        if (page < 0) page = 0;
        if (size < 1) size = 1;
        if (size > 100) size = 100;

        // parse status enum with a helpful error message
        JobStatus jobStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                jobStatus = JobStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid status value: '" + status + "'. Allowed values: " + Arrays.toString(JobStatus.values()));
            }
        }
        Page<Job> jobPage = jobService.listJobs(jobStatus, type == null || type.isBlank() ? null : type.trim(), page, size);

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
        jobService.cancelJob(id);
        return ResponseEntity.noContent().build();
    }
}