package com.farm.workers.controller;

import com.farm.workers.dto.WorkRecordDto;
import com.farm.workers.service.WorkRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/work-records")
@RequiredArgsConstructor
@Tag(name = "Work Records", description = "Seasonal work record management")
@SecurityRequirement(name = "bearerAuth")
public class WorkRecordController {

    private final WorkRecordService workRecordService;

    /**
     * GET /api/v1/work-records/worker/{workerId}
     * Get all work records for a specific worker
     */
    @GetMapping("/worker/{workerId}")
    @Operation(summary = "Get all work records for a worker")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkRecordDto.Response>> getWorkerRecords(
            @PathVariable String workerId) {

        return ResponseEntity.ok(workRecordService.getRecordsForWorker(workerId));
    }

    /**
     * GET /api/v1/work-records/worker/{workerId}/season/{season}
     * Get work records filtered by season - the main query pattern
     */
    @GetMapping("/worker/{workerId}/season/{season}")
    @Operation(summary = "Get work records for a worker in a specific season",
               description = "Season format: YYYY-SEASON e.g. 2024-HARVEST")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkRecordDto.Response>> getWorkerSeasonRecords(
            @PathVariable String workerId,
            @PathVariable String season) {

        return ResponseEntity.ok(workRecordService.getRecordsForWorkerAndSeason(workerId, season));
    }

    /**
     * GET /api/v1/work-records/worker/{workerId}/season/{season}/summary
     * Aggregated season summary - totals, stats
     */
    @GetMapping("/worker/{workerId}/season/{season}/summary")
    @Operation(summary = "Get season summary statistics for a worker")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkRecordDto.SeasonSummary> getSeasonSummary(
            @PathVariable String workerId,
            @PathVariable String season) {

        return ResponseEntity.ok(workRecordService.getSeasonSummary(workerId, season));
    }

    /**
     * POST /api/v1/work-records
     * Create a new work record for a worker's shift
     */
    @PostMapping
    @Operation(summary = "Log a work record / shift")
    @PreAuthorize("hasAuthority('SCOPE_records.write')")
    public ResponseEntity<WorkRecordDto.Response> createWorkRecord(
            @Valid @RequestBody WorkRecordDto.CreateRequest request) {

        WorkRecordDto.Response created = workRecordService.createWorkRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH /api/v1/work-records/{id}/approve
     * Partial update (status change) - approve a submitted record
     *
     * @PatchMapping  ≈  [HttpPatch] - for partial updates
     * vs @PutMapping ≈  [HttpPut]  - for full replacement
     */
    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve a submitted work record")
    @PreAuthorize("hasAuthority('SCOPE_records.approve')")
    public ResponseEntity<WorkRecordDto.Response> approveRecord(
            @PathVariable String id,
            @RequestParam(required = false) String notes) {

        return ResponseEntity.ok(workRecordService.approveWorkRecord(id, notes));
    }
}
