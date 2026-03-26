package com.example.Model.Dto.Response;

import com.example.Model.Status.AccountStatus;
import com.example.Model.Status.AccountType;
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
public class AccountResponse {
    private Long accountId;
    private String accountNumber;
    private Long userId;
    private AccountType accountType;
    private AccountStatus status;
    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal ledgerBalance;
    private BigDecimal minimumBalance;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
