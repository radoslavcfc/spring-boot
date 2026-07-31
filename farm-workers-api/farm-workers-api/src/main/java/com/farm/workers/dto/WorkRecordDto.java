package com.farm.workers.dto;

import com.farm.workers.model.WorkRecord;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;

public class WorkRecordDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "Worker ID is required")
        private String workerId;

        @NotBlank(message = "Season is required")
        @Pattern(regexp = "^\\d{4}-(SPRING|SUMMER|AUTUMN|HARVEST|WINTER)$",
                 message = "Season format must be YYYY-SEASON (e.g., 2024-HARVEST)")
        private String season;

        @NotBlank
        private String farmLocation;

        @NotBlank
        private String cropType;

        @NotNull
        private WorkRecord.WorkType workType;

        @NotNull
        private LocalDate workDate;

        @NotNull
        private LocalTime startTime;

        @NotNull
        private LocalTime endTime;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal hoursWorked;

        @DecimalMin(value = "0.0")
        private BigDecimal unitsHarvested;

        private String unitOfMeasure;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal hourlyRate;

        @DecimalMin(value = "0.0")
        private BigDecimal unitRate;

        private String supervisorNotes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String workerId;
        private String workerName;
        private String season;
        private String farmLocation;
        private String cropType;
        private WorkRecord.WorkType workType;
        private LocalDate workDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private BigDecimal hoursWorked;
        private BigDecimal unitsHarvested;
        private String unitOfMeasure;
        private BigDecimal hourlyRate;
        private BigDecimal unitRate;
        private BigDecimal totalPay;
        private WorkRecord.RecordStatus status;
        private String supervisorId;
        private String supervisorNotes;
        private boolean paymentProcessed;
        private String paymentReference;
        private Instant createdAt;
    }

    /**
     * Summary of a worker's season - aggregated stats
     * This gets computed by an Azure Function (see AzureFunctionService)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonSummary {
        private String workerId;
        private String workerName;
        private String season;
        private int totalDaysWorked;
        private BigDecimal totalHoursWorked;
        private BigDecimal totalUnitsHarvested;
        private BigDecimal totalEarnings;
        private WorkRecord.WorkType mostFrequentWorkType;
        private java.util.List<String> farmsWorkedAt;
    }
}
