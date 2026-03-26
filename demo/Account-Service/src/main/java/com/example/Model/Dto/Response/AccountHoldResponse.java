package com.example.Model.Dto.Response;

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
public class AccountHoldResponse {
    private Long holdId;
    private Long accountId;
    private BigDecimal amount;
    private String reason;
    private Long referenceId;
    private AccountHoldStatus status;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
}
