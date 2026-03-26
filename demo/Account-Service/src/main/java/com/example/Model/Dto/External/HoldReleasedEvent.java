package com.example.Model.Dto.External;

import com.example.Model.Status.AccountHoldStatus;
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
public class HoldReleasedEvent {
    private Long holdId;
    private Long accountId;
    private String accountNumber;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private Long referenceId;
    private AccountHoldStatus oldStatus;
    private AccountHoldStatus newStatus;
    private String requestedBy;
    private LocalDateTime releasedAt;
    private BigDecimal availableBalanceAfter;
    private BigDecimal ledgerBalanceAfter;
}
