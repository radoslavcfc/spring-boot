package com.farm.functions.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeasonPayrollResponse {
    private String workerId;
    private String season;
    private int totalDaysWorked;
    private BigDecimal totalHoursWorked;
    private BigDecimal totalEarnings;
}
