package com.example.webapi.domain;

import com.example.shared.dto.JobStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @Column(columnDefinition = "uniqueidentifier")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobStatus status;

    @Column(length = 4000)
    private String result;

    @Column(length = 4000)
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant completedAt;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus v) { this.status = v; }
    public String getResult() { return result; }
    public void setResult(String v) { this.result = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant v) { this.completedAt = v; }
}
