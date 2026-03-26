package com.example.Model.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SnapshotResponse {
    private Long snapshotId;
    private Long accountId;
    private LocalDate snapshotDate;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
}
