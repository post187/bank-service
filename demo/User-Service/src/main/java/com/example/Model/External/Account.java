package com.example.Model.External;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private Long accountId;

    private String numberAccount;

    private String accountType;

    private String accountStatus;

    private BigDecimal availableBalance;

    private Long userId;
}
