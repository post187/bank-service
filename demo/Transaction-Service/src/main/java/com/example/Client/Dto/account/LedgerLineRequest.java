package com.example.Client.Dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerLineRequest {
    private Long accountId;
    private EntryType entryType;
    private BigDecimal amount;
    private String currency;
}
