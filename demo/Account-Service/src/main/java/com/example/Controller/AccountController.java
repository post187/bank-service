package com.example.Controller;

import com.example.Model.Dto.Request.CloseAccountRequest;
import com.example.Model.Dto.Request.CreateAccountRequest;
import com.example.Model.Dto.Request.CreateHoldRequest;
import com.example.Model.Dto.Request.CreateJournalRequest;
import com.example.Model.Dto.Request.FreezeAccountRequest;
import com.example.Model.Dto.Request.ReleaseHoldRequest;
import com.example.Model.Dto.Request.UnfreezeAccountRequest;
import com.example.Model.Dto.Response.AccountBalanceResponse;
import com.example.Model.Dto.Response.AccountHoldResponse;
import com.example.Model.Dto.Response.AccountResponse;
import com.example.Model.Dto.Response.ApiResponse;
import com.example.Model.Dto.Response.LedgerEntryResponse;
import com.example.Model.Dto.Response.LedgerJournalResponse;
import com.example.Model.Dto.Response.SnapshotResponse;
import com.example.Model.Dto.Response.StatusHistoryResponse;
import com.example.Service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        return ResponseEntity.ok(accountService.createAccount(request));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getAccountBalance(accountId));
    }

    @PutMapping("/freeze")
    public ResponseEntity<ApiResponse> freezeAccount(@RequestBody FreezeAccountRequest request) {
        return ResponseEntity.ok(accountService.freezeAccount(request));
    }

    @PutMapping("/unfreeze")
    public ResponseEntity<ApiResponse> unfreezeAccount(@RequestBody UnfreezeAccountRequest request) {
        return ResponseEntity.ok(accountService.unfreezeAccount(request));
    }

    @PutMapping("/close")
    public ResponseEntity<ApiResponse> closeAccount(@RequestBody CloseAccountRequest request) {
        return ResponseEntity.ok(accountService.closeAccount(request));
    }

    @PostMapping("/journals")
    public ResponseEntity<LedgerJournalResponse> postJournal(@RequestBody CreateJournalRequest request) {
        return ResponseEntity.ok(accountService.postJournal(request));
    }

    @GetMapping("/{accountId}/ledger-entries")
    public ResponseEntity<List<LedgerEntryResponse>> getLedgerEntries(
            @PathVariable Long accountId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(accountService.getLedgerEntries(accountId, from, to));
    }

    @GetMapping("/journals/{journalId}")
    public ResponseEntity<LedgerJournalResponse> getJournalById(@PathVariable Long journalId) {
        return ResponseEntity.ok(accountService.getJournalById(journalId));
    }

    @GetMapping("/{accountId}/journals")
    public ResponseEntity<List<LedgerJournalResponse>> getJournals(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getJournals(accountId));
    }

    @GetMapping("/{accountId}/status-history")
    public ResponseEntity<List<StatusHistoryResponse>> getStatusHistory(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getStatusHistory(accountId));
    }

    @GetMapping("/{accountId}/status-history/filter")
    public ResponseEntity<List<StatusHistoryResponse>> getStatusHistoryByPeriod(
            @PathVariable Long accountId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(accountService.getStatusHistoryByPeriod(accountId, from, to));
    }

    @PostMapping("/holds")
    public ResponseEntity<AccountHoldResponse> createHold(@RequestBody CreateHoldRequest request) {
        return ResponseEntity.ok(accountService.createHold(request));
    }

    @PutMapping("/holds/release")
    public ResponseEntity<ApiResponse> releaseHold(@RequestBody ReleaseHoldRequest request) {
        return ResponseEntity.ok(accountService.releaseHold(request));
    }

    @GetMapping("/{accountId}/holds")
    public ResponseEntity<List<AccountHoldResponse>> getHolds(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getHolds(accountId));
    }

    @PostMapping("/{accountId}/snapshots")
    public ResponseEntity<SnapshotResponse> createDailySnapshot(
            @PathVariable Long accountId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(accountService.createDailySnapshot(accountId, date));
    }

    @GetMapping("/{accountId}/snapshots")
    public ResponseEntity<List<SnapshotResponse>> getDailySnapshots(
            @PathVariable Long accountId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(accountService.getDailySnapshots(accountId, from, to));
    }
}
