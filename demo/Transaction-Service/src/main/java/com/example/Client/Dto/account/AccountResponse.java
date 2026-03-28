package com.example.Client.Dto.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountResponse {
    private Long accountId;
    private String accountNumber;
    private Long userId;
    private String accountType;
    private String status;
    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal ledgerBalance;
    private BigDecimal minimumBalance;
}
