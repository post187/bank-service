package com.example.Service;

import com.example.Model.Dto.Request.CreateHoldRequest;
import com.example.Model.Dto.Request.ReleaseHoldRequest;
import com.example.Model.Dto.Response.AccountHoldResponse;
import com.example.Model.Dto.Response.ApiResponse;

import java.util.List;

public interface AccountHoldService {
    AccountHoldResponse createHold(CreateHoldRequest request);

    ApiResponse releaseHold(ReleaseHoldRequest request);

    List<AccountHoldResponse> getHolds(Long accountId);
}
