package com.example.Model.Dto.Response;

import com.example.Model.Enum.TransactionKind;
import com.example.Model.Enum.TxnWorkflowStatus;
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
public class TransactionResponse {
    private Long id;
    private String idempotencyKey;
    private TransactionKind kind;
    private TxnWorkflowStatus status;
    private BigDecimal amount;
    private String currency;
    private Long fromAccountId;
    private Long toAccountId;
    private Long initiatorUserId;
    private Long ledgerJournalId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
