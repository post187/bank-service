package com.example.Model.Dto.Request;

import com.example.Model.Status.EntryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LedgerLineRequest {
    private Long accountId;
    private EntryType entryType;
    private BigDecimal amount;
    private String currency;
}
