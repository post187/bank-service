package com.example.Model.Dto.Request;

import com.example.Model.Status.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateAccountRequest {
    private Long userId;
    private AccountType accountType;
    private String currency;
    private BigDecimal initialBalance;
    private BigDecimal minimumBalance;
    private String createdBy;
}
