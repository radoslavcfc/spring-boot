package com.farm.workers.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Work Record - tracks daily/shift work for each worker per season.
 *
 * ╔══════════════════════════════════════════════════════════╗
 * ║  CosmosDB Design Note (for .NET devs)                    ║
 * ║  Unlike SQL/EF Core, we store WorkRecords in their OWN  ║
 * ║  container (not embedded in Worker) because:            ║
 * ║  - Records grow unbounded over time                     ║
 * ║  - We query them independently by date range / season   ║
 * ║  - CosmosDB has 2MB document size limit                 ║
 * ║                                                          ║
 * ║  Partition Key = workerId (all records for one worker   ║
 * ║  land on the same partition = efficient queries)        ║
 * ╚══════════════════════════════════════════════════════════╝
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Container(containerName = "work-records")
public class WorkRecord {

    @Id
    private String id;

    @PartitionKey
    private String workerId;            // References Worker.id (no FK in NoSQL!)

    private String workerName;          // Denormalized for read performance
                                        // ≈ common NoSQL pattern vs SQL joins

    private String season;              // e.g., "2024-SPRING", "2024-HARVEST"
    private String farmLocation;        // Farm field / zone
    private String cropType;            // e.g., "Strawberry", "Wheat", "Grape"
    private WorkType workType;

    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;

    /**
     * BigDecimal  ≈  decimal in C#
     * NEVER use double/float for money/precise quantities in Java
     * (same rule as in C# - use decimal, not double for financial values)
     */
    private BigDecimal hoursWorked;
    private BigDecimal unitsHarvested;  // e.g., kg of fruit picked
    private String unitOfMeasure;       // "kg", "crates", "rows"

    private BigDecimal hourlyRate;
    private BigDecimal unitRate;
    private BigDecimal totalPay;        // Calculated: hours * hourlyRate + units * unitRate

    private RecordStatus status;
    private String supervisorId;
    private String supervisorNotes;
    private boolean paymentProcessed;
    private String paymentReference;    // Set by Azure Function after payment processing

    private String createdBy;
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;

    public enum WorkType {
        PLANTING,
        IRRIGATION,
        HARVESTING,
        PRUNING,
        WEEDING,
        PACKING,
        EQUIPMENT_OPERATION,
        SUPERVISION,
        OTHER
    }

    public enum RecordStatus {
        DRAFT,
        SUBMITTED,
        APPROVED,
        REJECTED,
        PAYMENT_PENDING,
        PAYMENT_PROCESSED
    }
}
