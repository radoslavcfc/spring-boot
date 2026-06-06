package com.example.webapi.controller;

import com.example.shared.dto.JobStatus;
import com.example.webapi.domain.Job;
import com.example.webapi.job.JobService;
import com.example.webapi.repository.JobRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Long-running operation: submit (202) + poll (200/202)")
public class JobController {

    private final JobRepository repo;
    private final JobService service;

    public JobController(JobRepository repo, JobService service) {
        this.repo = repo;
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Submit a long-running job. Returns 202 with Location header.")
    public ResponseEntity<Job> submit(@RequestBody(required = false) String input) {
        Job j = new Job();
        j.setId(UUID.randomUUID());
        j.setStatus(JobStatus.PENDING);
        j.setCreatedAt(Instant.now());
        repo.save(j);
        service.runAsync(j.getId(), input == null ? "" : input);
        return ResponseEntity.accepted()
            .location(URI.create("/api/jobs/" + j.getId()))
            .body(j);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Poll job status. 200 when terminal, 202 while running.")
    public ResponseEntity<Job> get(@PathVariable UUID id) {
        return repo.findById(id).map(j -> {
            if (j.getStatus() == JobStatus.PENDING || j.getStatus() == JobStatus.RUNNING) {
                return ResponseEntity.accepted().body(j);
            }
            return ResponseEntity.ok(j);
        }).orElse(ResponseEntity.notFound().build());
    }
}
