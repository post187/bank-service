package com.example.Model.Dto.Response;

import com.example.Model.Status.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountBalanceResponse {
    private Long accountId;
    private String accountNumber;
    private String currency;
    private AccountStatus status;
    private BigDecimal availableBalance;
    private BigDecimal ledgerBalance;
    private BigDecimal heldAmount;
}
