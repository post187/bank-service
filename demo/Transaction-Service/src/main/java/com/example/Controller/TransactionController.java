package com.example.Controller;

import com.example.Model.Dto.Request.DepositRequest;
import com.example.Model.Dto.Request.TransferRequest;
import com.example.Model.Dto.Request.WithdrawRequest;
import com.example.Model.Dto.Response.TransactionResponse;
import com.example.Service.TransactionProcessingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionProcessingService transactionProcessingService;

    @PostMapping("/deposits")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TransactionResponse> deposit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest request
    ) {
        return ResponseEntity.ok(transactionProcessingService.deposit(request, idempotencyKey.trim()));
    }

    @PostMapping("/withdrawals")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TransactionResponse> withdraw(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WithdrawRequest request
    ) {
        return ResponseEntity.ok(transactionProcessingService.withdraw(request, idempotencyKey.trim()));
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        return ResponseEntity.ok(transactionProcessingService.transfer(request, idempotencyKey.trim()));
    }
}
