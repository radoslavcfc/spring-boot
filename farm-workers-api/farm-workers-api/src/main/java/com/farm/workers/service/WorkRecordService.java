package com.farm.workers.service;

import com.farm.workers.azure.StorageQueueService;
import com.farm.workers.dto.WorkRecordDto;
import com.farm.workers.exception.BusinessRuleException;
import com.farm.workers.exception.ResourceNotFoundException;
import com.farm.workers.model.WorkRecord;
import com.farm.workers.model.Worker;
import com.farm.workers.repository.WorkRecordRepository;
import com.farm.workers.repository.WorkerRepository;
import com.farm.workers.util.WorkerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkRecordService {

    private final WorkRecordRepository workRecordRepository;
    private final WorkerRepository workerRepository;
    private final WorkerMapper workerMapper;
    private final StorageQueueService storageQueueService;

    public List<WorkRecordDto.Response> getRecordsForWorker(String workerId) {
        // Verify worker exists first
        if (!workerRepository.existsById(workerId)) {
            throw new ResourceNotFoundException("Worker", "id", workerId);
        }

        return workRecordRepository.findByWorkerId(workerId)
                .stream()
                .map(workerMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<WorkRecordDto.Response> getRecordsForWorkerAndSeason(String workerId, String season) {
        return workRecordRepository.findByWorkerIdAndSeason(workerId, season)
                .stream()
                .map(workerMapper::toResponse)
                .collect(Collectors.toList());
    }

    public WorkRecordDto.Response createWorkRecord(WorkRecordDto.CreateRequest request) {
        log.info("Creating work record for worker: {} season: {}", request.getWorkerId(), request.getSeason());

        // Validate worker exists and is active
        Worker worker = workerRepository.findById(request.getWorkerId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker", "id", request.getWorkerId()));

        if (worker.getStatus() != Worker.WorkerStatus.ACTIVE) {
            throw new BusinessRuleException(
                "Cannot create work record for worker with status: " + worker.getStatus());
        }

        WorkRecord record = workerMapper.toEntity(request);
        record.setId(UUID.randomUUID().toString());
        record.setWorkerName(worker.getFirstName() + " " + worker.getLastName());  // Denormalize
        record.setTotalPay(calculateTotalPay(request));
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        record.setCreatedBy(getCurrentUserId());

        WorkRecord saved = workRecordRepository.save(record);

        // Queue a message for async processing (payroll notification etc.)
        // ≈ adding a message to Azure Queue Storage (like BackgroundService trigger)
        storageQueueService.enqueueWorkRecordCreated(saved.getId(), saved.getWorkerId());

        return workerMapper.toResponse(saved);
    }

    /**
     * Approve a work record (supervisor action)
     */
    public WorkRecordDto.Response approveWorkRecord(String recordId, String supervisorNotes) {
        WorkRecord record = workRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkRecord", "id", recordId));

        if (record.getStatus() != WorkRecord.RecordStatus.SUBMITTED) {
            throw new BusinessRuleException(
                "Can only approve records in SUBMITTED status. Current: " + record.getStatus());
        }

        record.setStatus(WorkRecord.RecordStatus.APPROVED);
        record.setSupervisorId(getCurrentUserId());
        record.setSupervisorNotes(supervisorNotes);
        record.setUpdatedAt(Instant.now());

        return workerMapper.toResponse(workRecordRepository.save(record));
    }

    /**
     * Get season summary for a worker
     * Aggregates data from all work records for the season
     *
     * @Async runs this in a separate thread pool
     * ≈ Task.Run() or async/await in .NET
     * (here we return the result synchronously, but could return CompletableFuture<T>)
     */
    public WorkRecordDto.SeasonSummary getSeasonSummary(String workerId, String season) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker", "id", workerId));

        List<WorkRecord> records = workRecordRepository.findByWorkerIdAndSeason(workerId, season);

        if (records.isEmpty()) {
            return WorkRecordDto.SeasonSummary.builder()
                    .workerId(workerId)
                    .workerName(worker.getFirstName() + " " + worker.getLastName())
                    .season(season)
                    .totalDaysWorked(0)
                    .totalHoursWorked(BigDecimal.ZERO)
                    .totalUnitsHarvested(BigDecimal.ZERO)
                    .totalEarnings(BigDecimal.ZERO)
                    .build();
        }

        // Java streams aggregation  ≈  LINQ .Sum(), .GroupBy(), etc.
        BigDecimal totalHours = records.stream()
                .map(WorkRecord::getHoursWorked)     // method reference  ≈  r => r.HoursWorked
                .filter(h -> h != null)              // .Where(h => h != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);  // .Sum()

        BigDecimal totalUnits = records.stream()
                .map(WorkRecord::getUnitsHarvested)
                .filter(u -> u != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPay = records.stream()
                .map(WorkRecord::getTotalPay)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Find most frequent work type using grouping
        // ≈ records.GroupBy(r => r.WorkType).OrderByDescending(g => g.Count()).First().Key
        WorkRecord.WorkType mostFrequent = records.stream()
                .collect(Collectors.groupingBy(WorkRecord::getWorkType, Collectors.counting()))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);

        List<String> farms = records.stream()
                .map(WorkRecord::getFarmLocation)
                .distinct()          // ≈ .Distinct()
                .collect(Collectors.toList());

        long uniqueDays = records.stream()
                .map(WorkRecord::getWorkDate)
                .distinct()
                .count();

        return WorkRecordDto.SeasonSummary.builder()
                .workerId(workerId)
                .workerName(worker.getFirstName() + " " + worker.getLastName())
                .season(season)
                .totalDaysWorked((int) uniqueDays)
                .totalHoursWorked(totalHours)
                .totalUnitsHarvested(totalUnits)
                .totalEarnings(totalPay)
                .mostFrequentWorkType(mostFrequent)
                .farmsWorkedAt(farms)
                .build();
    }

    /**
     * Calculate pay: hours * hourly rate + units * unit rate
     * Using BigDecimal throughout to avoid floating point errors
     * ≈ always using decimal in C# for monetary calculations
     */
    private BigDecimal calculateTotalPay(WorkRecordDto.CreateRequest request) {
        BigDecimal hourPay = request.getHoursWorked()
                .multiply(request.getHourlyRate())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal unitPay = BigDecimal.ZERO;
        if (request.getUnitsHarvested() != null && request.getUnitRate() != null) {
            unitPay = request.getUnitsHarvested()
                    .multiply(request.getUnitRate())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return hourPay.add(unitPay);
    }

    private String getCurrentUserId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "system";
        } catch (Exception e) {
            return "system";
        }
    }
}
