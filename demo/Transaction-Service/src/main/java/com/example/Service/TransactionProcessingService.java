package com.example.Service;

import com.example.Model.Dto.Request.DepositRequest;
import com.example.Model.Dto.Request.TransferRequest;
import com.example.Model.Dto.Request.WithdrawRequest;
import com.example.Model.Dto.Response.TransactionResponse;

public interface TransactionProcessingService {

    TransactionResponse deposit(DepositRequest request, String idempotencyKey);

    TransactionResponse withdraw(WithdrawRequest request, String idempotencyKey);

    TransactionResponse transfer(TransferRequest request, String idempotencyKey);
}
