package com.example.Model.Dto.Request;

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
public class CreateHoldRequest {
    private Long accountId;
    private BigDecimal amount;
    private String reason;
    private Long referenceId;
    private String requestedBy;
    private LocalDateTime expiredAt;
}
