package com.example.Model.Dto.External;

import com.example.Model.Enum.TransactionKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCompletedEvent {
    private Long transactionId;
    private TransactionKind kind;
    private Long initiatorUserId;
    private Long ledgerJournalId;
    private BigDecimal amount;
    private String currency;
    private Long fromAccountId;
    private Long toAccountId;
    private LocalDateTime completedAt;
}
