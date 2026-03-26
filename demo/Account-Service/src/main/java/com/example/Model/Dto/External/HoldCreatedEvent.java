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
public class HoldCreatedEvent {
    private Long holdId;
    private Long accountId;
    private String accountNumber;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private Long referenceId;
    private AccountHoldStatus status;
    private String requestedBy;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private BigDecimal availableBalanceAfter;
    private BigDecimal ledgerBalanceAfter;
}
