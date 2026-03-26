package com.example.Service;

import com.example.Model.Dto.Request.CloseAccountRequest;
import com.example.Model.Dto.Request.CreateAccountRequest;
import com.example.Model.Dto.Request.FreezeAccountRequest;
import com.example.Model.Dto.Request.UnfreezeAccountRequest;
import com.example.Model.Dto.Response.AccountBalanceResponse;
import com.example.Model.Dto.Response.AccountResponse;
import com.example.Model.Dto.Response.ApiResponse;
import com.example.Model.Dto.Response.StatusHistoryResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface AccountService extends LedgerService, StatusHistoryService, SnapshotService, AccountHoldService {
    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccountById(Long accountId);

    AccountResponse getAccountByNumber(String accountNumber);

    List<AccountResponse> getAccountsByUserId(Long userId);

    AccountBalanceResponse getAccountBalance(Long accountId);

    ApiResponse freezeAccount(FreezeAccountRequest request);

    ApiResponse unfreezeAccount(UnfreezeAccountRequest request);

    ApiResponse closeAccount(CloseAccountRequest request);

    @Override
    List<StatusHistoryResponse> getStatusHistory(Long accountId);

    @Override
    List<StatusHistoryResponse> getStatusHistoryByPeriod(Long accountId, LocalDateTime from, LocalDateTime to);
}
