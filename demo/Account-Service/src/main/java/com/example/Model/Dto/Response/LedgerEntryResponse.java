package com.example.Model.Dto.Response;

import com.example.Model.Status.EntryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LedgerEntryResponse {
    private Long entryId;
    private Long journalId;
    private Long accountId;
    private String accountNumber;
    private EntryType entryType;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;
}
