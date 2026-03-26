package com.example.Model.Dto.External;

import com.example.Model.Status.EntryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LedgerJournalPostedEntryEvent {
    private Long entryId;
    private Long accountId;
    private String accountNumber;
    private EntryType entryType;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
}
