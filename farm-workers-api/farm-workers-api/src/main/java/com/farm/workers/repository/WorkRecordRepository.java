package com.farm.workers.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import com.farm.workers.model.WorkRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkRecordRepository extends CosmosRepository<WorkRecord, String> {

    /**
     * All records for a worker (efficient: hits a single partition)
     * ≈ _context.WorkRecords.Where(r => r.WorkerId == workerId)
     */
    List<WorkRecord> findByWorkerId(String workerId);

    /**
     * All records for a specific season
     * ≈ _context.WorkRecords.Where(r => r.Season == season)
     */
    List<WorkRecord> findBySeason(String season);

    /**
     * Records by worker AND season - most common access pattern
     * Efficient because workerId is the partition key
     */
    List<WorkRecord> findByWorkerIdAndSeason(String workerId, String season);

    /**
     * Records by worker in a date range
     * Spring Data supports Between for ranges automatically
     * ≈ .Where(r => r.WorkDate >= start && r.WorkDate <= end)
     */
    List<WorkRecord> findByWorkerIdAndWorkDateBetween(
        String workerId, LocalDate startDate, LocalDate endDate);

    /**
     * Pending approval records (cross-partition query - use sparingly)
     */
    List<WorkRecord> findByStatus(WorkRecord.RecordStatus status);

    /**
     * Records awaiting payment processing
     * ≈ .Where(r => !r.PaymentProcessed && r.Status == APPROVED)
     */
    @Query("SELECT * FROM c WHERE c.status = 'APPROVED' AND c.paymentProcessed = false")
    List<WorkRecord> findUnprocessedApprovedRecords();

    /**
     * Count records per worker per season (for summary stats)
     */
    long countByWorkerIdAndSeason(String workerId, String season);

    List<WorkRecord> findByWorkerIdAndStatus(String workerId, WorkRecord.RecordStatus status);
}
