package com.example.Service;

import com.example.Model.Dto.Response.StatusHistoryResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface StatusHistoryService {
    List<StatusHistoryResponse> getStatusHistory(Long accountId);
    List<StatusHistoryResponse> getStatusHistoryByPeriod(Long accountId, LocalDateTime from, LocalDateTime to);
}
