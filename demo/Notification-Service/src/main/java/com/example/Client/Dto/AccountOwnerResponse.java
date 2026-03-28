package com.example.Client.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountOwnerResponse {
    private Long accountId;
    private Long userId;
    private String accountNumber;
    private String currency;
}
