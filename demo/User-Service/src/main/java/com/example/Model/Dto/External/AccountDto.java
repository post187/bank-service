package com.example.Model.Dto.External;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {
    private String accountNumber;

    private BigDecimal bigDecimal;

    private Integer id;

    private String type;

    private String status;

    private BigDecimal availableBalance;
}
