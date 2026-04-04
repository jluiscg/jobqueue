package com.lcortes.jobqueue.api;

import com.lcortes.jobqueue.api.dto.JobResponse;
import com.lcortes.jobqueue.api.dto.PagedResponse;
import com.lcortes.jobqueue.api.dto.SubmitJobRequest;
import com.lcortes.jobqueue.domain.Job;
import com.lcortes.jobqueue.domain.JobStatus;
import com.lcortes.jobqueue.engine.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0) page = 0;
        if (size < 1) size = 1;
        if (size > 100) size = 100;

        Page<Job> jobPage = jobService.listJobs(status, type, page, size);

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