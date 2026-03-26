package com.example.Model.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatusHistoryResponse {
    private Long id;
    private Long accountId;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private String changedBy;
    private LocalDateTime changedAt;
}
