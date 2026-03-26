package com.example.Model.Dto.External;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountSnapshotCreatedEvent {
    private Long snapshotId;
    private Long accountId;
    private String accountNumber;
    private Long userId;
    private LocalDate snapshotDate;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private String currency;
    private LocalDateTime createdAt;
}
