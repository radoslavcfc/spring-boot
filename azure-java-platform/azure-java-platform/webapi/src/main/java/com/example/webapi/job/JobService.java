package com.example.webapi.job;

import com.example.shared.dto.JobStatus;
import com.example.webapi.domain.Job;
import com.example.webapi.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JobService {
    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private final JobRepository repo;

    public JobService(JobRepository repo) { this.repo = repo; }

    @Async("jobExecutor")
    public void runAsync(UUID id, String input) {
        Job j = repo.findById(id).orElseThrow();
        j.setStatus(JobStatus.RUNNING);
        repo.save(j);
        log.info("job {} started", id);
        try {
            // Simulated long-running work
            Thread.sleep(10_000);
            j.setStatus(JobStatus.SUCCEEDED);
            j.setResult("processed: " + input.length() + " chars");
        } catch (Exception e) {
            j.setStatus(JobStatus.FAILED);
            j.setErrorMessage(e.getMessage());
            log.error("job {} failed", id, e);
        } finally {
            j.setCompletedAt(Instant.now());
            repo.save(j);
            log.info("job {} finished status={}", id, j.getStatus());
        }
    }
}
