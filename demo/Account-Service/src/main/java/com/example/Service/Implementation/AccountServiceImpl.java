package com.example.Service.Implementation;

import com.example.Client.Dto.UserAccountEligibilityResponse;
import com.example.Client.UserServiceClient;
import com.example.Model.Dto.External.AccountSnapshotCreatedEvent;
import com.example.Model.Dto.External.HoldCreatedEvent;
import com.example.Model.Dto.External.HoldReleasedEvent;
import com.example.Model.Dto.External.LedgerJournalPostedEntryEvent;
import com.example.Model.Dto.External.LedgerJournalPostedEvent;
import com.example.Model.Dto.Request.CloseAccountRequest;
import com.example.Model.Dto.Request.CreateAccountRequest;
import com.example.Model.Dto.Request.CreateHoldRequest;
import com.example.Model.Dto.Request.CreateJournalRequest;
import com.example.Model.Dto.Request.FreezeAccountRequest;
import com.example.Model.Dto.Request.LedgerLineRequest;
import com.example.Model.Dto.Request.ReleaseHoldRequest;
import com.example.Model.Dto.Request.UnfreezeAccountRequest;
import com.example.Model.Dto.Response.AccountBalanceResponse;
import com.example.Model.Dto.Response.AccountHoldResponse;
import com.example.Model.Dto.Response.AccountOwnerInternalResponse;
import com.example.Model.Dto.Response.AccountResponse;
import com.example.Model.Dto.Response.ApiResponse;
import com.example.Model.Dto.Response.LedgerEntryResponse;
import com.example.Model.Dto.Response.LedgerJournalResponse;
import com.example.Model.Dto.Response.SnapshotResponse;
import com.example.Model.Dto.Response.StatusHistoryResponse;
import com.example.Model.Entity.Account;
import com.example.Model.Entity.AccountHold;
import com.example.Model.Entity.AccountStatusHistory;
import com.example.Model.Entity.DailyBalanceSnapshot;
import com.example.Model.Entity.LedgerEntry;
import com.example.Model.Entity.LedgerJournal;
import com.example.Model.Status.AccountHoldStatus;
import com.example.Model.Status.AccountStatus;
import com.example.Model.Status.EntryType;
import com.example.Model.Status.JournalStatus;
import com.example.Repository.AccountHoldRepository;
import com.example.Repository.AccountRepository;
import com.example.Repository.AccountStatusHistoryRepository;
import com.example.Repository.DailyBalanceSnapshotRepository;
import com.example.Repository.LedgerEntryRepository;
import com.example.Repository.LedgerJournalRepository;
import com.example.Service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountStatusHistoryRepository statusHistoryRepository;
    private final LedgerJournalRepository ledgerJournalRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountHoldRepository accountHoldRepository;
    private final DailyBalanceSnapshotRepository dailyBalanceSnapshotRepository;
    private final UserServiceClient userServiceClient;
    private final KafkaTemplate<String, Object> kafka;

    @Value("${spring.application.success:200}")
    private String responseCodeSuccess;

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        validateCreateAccountRequest(request);
        validateUserEligibility(request.getUserId());

        BigDecimal initialBalance = normalizedMoney(request.getInitialBalance());
        BigDecimal minimumBalance = normalizedMoney(request.getMinimumBalance());
        LocalDateTime now = LocalDateTime.now();

        Account account = Account.builder()
                .accountNumber(generateAccountNumber(request.getUserId()))
                .userId(request.getUserId())
                .accountType(request.getAccountType())
                .status(AccountStatus.ACTIVE)
                .currency(request.getCurrency().trim().toUpperCase())
                .availableBalance(initialBalance)
                .ledgerBalance(initialBalance)
                .minimumBalance(minimumBalance)
                .openedAt(now)
                .build();

        Account saved = accountRepository.save(account);
        statusHistoryRepository.save(AccountStatusHistory.builder()
                .account(saved)
                .oldStatus(null)
                .newStatus(AccountStatus.ACTIVE.name())
                .reason("Account created")
                .changedBy(blankToDefault(request.getCreatedBy(), "SYSTEM"))
                .changedAt(now)
                .build());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafka.send("account-created", request.getUserId().toString(), "Account created successfully. Your number account + " + account.getAccountNumber());
            }
        });

        return toAccountResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId) {
        return toAccountResponse(getAccount(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountOwnerInternalResponse getAccountOwnerInternal(Long accountId) {
        Account account = getAccount(accountId);
        return AccountOwnerInternalResponse.builder()
                .accountId(account.getAccountId())
                .userId(account.getUserId())
                .accountNumber(account.getAccountNumber())
                .currency(account.getCurrency())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return toAccountResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toAccountResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountBalanceResponse getAccountBalance(Long accountId) {
        Account account = getAccount(accountId);
        BigDecimal heldAmount = accountHoldRepository.findByAccount_AccountIdAndStatus(accountId, AccountHoldStatus.ACTIVE)
                .stream()
                .map(AccountHold::getAmount)
                .reduce(ZERO, BigDecimal::add);

        return AccountBalanceResponse.builder()
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .availableBalance(normalizedMoney(account.getAvailableBalance()))
                .ledgerBalance(normalizedMoney(account.getLedgerBalance()))
                .heldAmount(normalizedMoney(heldAmount))
                .build();
    }

    @Override
    @Transactional
    public ApiResponse freezeAccount(FreezeAccountRequest request) {
        Account account = getAccount(request.getAccountId());
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Closed account cannot be frozen");
        }
        if (account.getStatus() == AccountStatus.FROZEN) {
            return success("Account is already frozen");
        }

        AccountStatus oldStatus = account.getStatus();
        account.setStatus(AccountStatus.FROZEN);
        accountRepository.save(account);
        saveStatusHistory(account, oldStatus, AccountStatus.FROZEN, request.getReason(), request.getRequestedBy());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafka.send("account-status-changed", account.getUserId().toString(), "Account frozen successfully. " + request.getReason());
            }
        });

        return success("Account frozen successfully");
    }

    @Override
    @Transactional
    public ApiResponse unfreezeAccount(UnfreezeAccountRequest request) {
        Account account = getAccount(request.getAccountId());
        if (account.getStatus() != AccountStatus.FROZEN) {
            throw new IllegalStateException("Only frozen account can be unfrozen");
        }

        AccountStatus oldStatus = account.getStatus();
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);
        saveStatusHistory(account, oldStatus, AccountStatus.ACTIVE, "Account unfrozen", request.getRequestedBy());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafka.send("account-status-changed", account.getUserId().toString(), "Account unfrozen successfully");
            }
        });

        return success("Account unfrozen successfully");
    }

    @Override
    @Transactional
    public ApiResponse closeAccount(CloseAccountRequest request) {
        Account account = getAccount(request.getAccountId());
        if (account.getStatus() == AccountStatus.CLOSED) {
            return success("Account already closed");
        }
        if (normalizedMoney(account.getLedgerBalance()).compareTo(ZERO) != 0
                || normalizedMoney(account.getAvailableBalance()).compareTo(ZERO) != 0) {
            throw new IllegalStateException("Account balance must be zero before closing");
        }
        if (!accountHoldRepository.findByAccount_AccountIdAndStatus(account.getAccountId(), AccountHoldStatus.ACTIVE).isEmpty()) {
            throw new IllegalStateException("Active holds must be released before closing account");
        }

        AccountStatus oldStatus = account.getStatus();
        account.setStatus(AccountStatus.CLOSED);
        account.setClosedAt(LocalDateTime.now());
        accountRepository.save(account);
        saveStatusHistory(account, oldStatus, AccountStatus.CLOSED, request.getReason(), request.getRequestedBy());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafka.send("account-closed", account.getUserId().toString(), "Account closed successfully. " + request.getReason());
            }
        });

        return success("Account closed successfully");
    }

    @Override
    @Transactional
    public LedgerJournalResponse postJournal(CreateJournalRequest request) {
        validateJournalRequest(request);

        if (request.getReferenceType() != null && request.getReferenceId() != null) {
            ledgerJournalRepository.findByReferenceTypeAndReferenceId(request.getReferenceType(), request.getReferenceId())
                    .ifPresent(existing -> {
                        throw new IllegalStateException("Reference already posted");
                    });
        }

        BigDecimal totalDebit = ZERO;
        BigDecimal totalCredit = ZERO;

        for (LedgerLineRequest line : request.getEntries()) {
            BigDecimal amount = normalizedMoney(line.getAmount());
            if (amount.compareTo(ZERO) <= 0) {
                throw new IllegalArgumentException("Entry amount must be greater than zero");
            }
            if (line.getEntryType() == EntryType.DEBIT) {
                totalDebit = totalDebit.add(amount);
            } else {
                totalCredit = totalCredit.add(amount);
            }
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException("Total debit must equal total credit");
        }

        LocalDateTime now = LocalDateTime.now();
        LedgerJournal journal = LedgerJournal.builder()
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .description(request.getDescription())
                .status(JournalStatus.PENDING)
                .createdBy(blankToDefault(request.getCreatedBy(), "SYSTEM"))
                .createdAt(now)
                .entries(new ArrayList<>())
                .build();
        ledgerJournalRepository.save(journal);

        TreeSet<Long> accountIds = new TreeSet<>(Comparator.naturalOrder());
        for (LedgerLineRequest line : request.getEntries()) {
            accountIds.add(line.getAccountId());
        }
        Map<Long, Account> lockedAccounts = new LinkedHashMap<>();
        for (Long accountId : accountIds) {
            Account locked = accountRepository.findByIdForUpdate(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));
            lockedAccounts.put(accountId, locked);
        }

        List<LedgerEntry> persistedEntries = new ArrayList<>();
        for (LedgerLineRequest line : request.getEntries()) {
            Account account = lockedAccounts.get(line.getAccountId());
            ensureAccountCanPost(account, line.getEntryType());

            BigDecimal amount = normalizedMoney(line.getAmount());
            BigDecimal balanceBefore = normalizedMoney(account.getLedgerBalance());
            BigDecimal balanceAfter = line.getEntryType() == EntryType.DEBIT
                    ? balanceBefore.subtract(amount)
                    : balanceBefore.add(amount);

            if (line.getEntryType() == EntryType.DEBIT && balanceAfter.compareTo(normalizedMoney(account.getMinimumBalance()).negate()) < 0) {
                throw new IllegalStateException("Insufficient balance for debit entry");
            }

            account.setLedgerBalance(balanceAfter);
            account.setAvailableBalance(
                    line.getEntryType() == EntryType.DEBIT
                            ? normalizedMoney(account.getAvailableBalance()).subtract(amount)
                            : normalizedMoney(account.getAvailableBalance()).add(amount)
            );
            accountRepository.save(account);

            LedgerEntry entry = LedgerEntry.builder()
                    .journal(journal)
                    .account(account)
                    .entryType(line.getEntryType())
                    .amount(amount)
                    .currency(line.getCurrency() == null ? account.getCurrency() : line.getCurrency().trim().toUpperCase())
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .createdAt(now)
                    .build();
            persistedEntries.add(ledgerEntryRepository.save(entry));
        }

        journal.setStatus(JournalStatus.POSTED);
        journal.setPostedAt(now);
        journal.setEntries(persistedEntries);
        ledgerJournalRepository.save(journal);

        LedgerJournalPostedEvent journalPostedEvent = LedgerJournalPostedEvent.builder()
                .journalId(journal.getJournalId())
                .referenceType(journal.getReferenceType())
                .referenceId(journal.getReferenceId())
                .description(journal.getDescription())
                .createdBy(journal.getCreatedBy())
                .createdAt(journal.getCreatedAt())
                .postedAt(journal.getPostedAt())
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .entries(persistedEntries.stream()
                        .map(entry -> LedgerJournalPostedEntryEvent.builder()
                                .entryId(entry.getEntryId())
                                .accountId(entry.getAccount().getAccountId())
                                .accountNumber(entry.getAccount().getAccountNumber())
                                .entryType(entry.getEntryType())
                                .amount(normalizedMoney(entry.getAmount()))
                                .currency(entry.getCurrency())
                                .balanceBefore(normalizedMoney(entry.getBalanceBefore()))
                                .balanceAfter(normalizedMoney(entry.getBalanceAfter()))
                                .build())
                        .toList())
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafka.send("ledger-journal-posted", journal.getJournalId().toString(), journalPostedEvent);
            }
        });

        return toLedgerJournalResponse(journal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getLedgerEntries(Long accountId, LocalDateTime from, LocalDateTime to) {
        List<LedgerEntry> entries;
        if (from == null || to == null) {
            entries = ledgerEntryRepository.findByAccount_AccountIdOrderByCreatedAtDesc(accountId);
        } else {
            entries = ledgerEntryRepository.findByAccount_AccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(accountId, from, to);
        }
        return entries.stream().map(this::toLedgerEntryResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerJournalResponse getJournalById(Long journalId) {
        LedgerJournal journal = ledgerJournalRepository.findById(journalId)
                .orElseThrow(() -> new IllegalArgumentException("Journal not found"));
        return toLedgerJournalResponse(journal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerJournalResponse> getJournals(Long accountId) {
        return ledgerJournalRepository.findDistinctByEntries_Account_AccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toLedgerJournalResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getStatusHistory(Long accountId) {
        return statusHistoryRepository.findByAccount_AccountIdOrderByChangedAtDesc(accountId)
                .stream()
                .map(this::toStatusHistoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getStatusHistoryByPeriod(Long accountId, LocalDateTime from, LocalDateTime to) {
        return statusHistoryRepository
                .findByAccount_AccountIdAndChangedAtBetweenOrderByChangedAtDesc(accountId, from, to)
                .stream()
                .map(this::toStatusHistoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public AccountHoldResponse createHold(CreateHoldRequest request) {
        Account account = getAccount(request.getAccountId());
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Only active account can create hold");
        }

        BigDecimal amount = normalizedMoney(request.getAmount());
        if (amount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Hold amount must be greater than zero");
        }
        if (normalizedMoney(account.getAvailableBalance()).compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available balance for hold");
        }

        account.setAvailableBalance(normalizedMoney(account.getAvailableBalance()).subtract(amount));
        accountRepository.save(account);

        AccountHold hold = AccountHold.builder()
                .account(account)
                .amount(amount)
                .reason(request.getReason())
                .referenceId(request.getReferenceId())
                .status(AccountHoldStatus.ACTIVE)
                .expiredAt(request.getExpiredAt())
                .createdAt(LocalDateTime.now())
                .build();
        AccountHold savedHold = accountHoldRepository.save(hold);

        HoldCreatedEvent holdCreatedEvent = HoldCreatedEvent.builder()
                .holdId(savedHold.getHoldId())
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .amount(normalizedMoney(savedHold.getAmount()))
                .currency(account.getCurrency())
                .reason(savedHold.getReason())
                .referenceId(savedHold.getReferenceId())
                .status(savedHold.getStatus())
                .requestedBy(request.getRequestedBy())
                .expiredAt(savedHold.getExpiredAt())
                .createdAt(savedHold.getCreatedAt())
                .availableBalanceAfter(normalizedMoney(account.getAvailableBalance()))
                .ledgerBalanceAfter(normalizedMoney(account.getLedgerBalance()))
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafka.send("hold-created", savedHold.getHoldId().toString(), holdCreatedEvent);
            }
        });
        return toAccountHoldResponse(savedHold);
    }

    @Override
    @Transactional
    public ApiResponse releaseHold(ReleaseHoldRequest request) {
        AccountHold hold = accountHoldRepository.findById(request.getHoldId())
                .orElseThrow(() -> new IllegalArgumentException("Hold not found"));
        if (hold.getStatus() != AccountHoldStatus.ACTIVE) {
            throw new IllegalStateException("Only active hold can be released");
        }

        Account account = hold.getAccount();
        account.setAvailableBalance(normalizedMoney(account.getAvailableBalance()).add(normalizedMoney(hold.getAmount())));
        accountRepository.save(account);

        AccountHoldStatus oldStatus = hold.getStatus();
        hold.setStatus(AccountHoldStatus.RELEASED);
        accountHoldRepository.save(hold);

        HoldReleasedEvent holdReleasedEvent = HoldReleasedEvent.builder()
                .holdId(hold.getHoldId())
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .amount(normalizedMoney(hold.getAmount()))
                .currency(account.getCurrency())
                .referenceId(hold.getReferenceId())
                .oldStatus(oldStatus)
                .newStatus(hold.getStatus())
                .requestedBy(request.getRequestedBy())
                .releasedAt(LocalDateTime.now())
                .availableBalanceAfter(normalizedMoney(account.getAvailableBalance()))
                .ledgerBalanceAfter(normalizedMoney(account.getLedgerBalance()))
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafka.send("hold-released", hold.getHoldId().toString(), holdReleasedEvent);
            }
        });

        return success("Hold released successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountHoldResponse> getHolds(Long accountId) {
        return accountHoldRepository.findByAccount_AccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toAccountHoldResponse)
                .toList();
    }

    @Override
    @Transactional
    public SnapshotResponse createDailySnapshot(Long accountId, LocalDate date) {
        Account account = getAccount(accountId);
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay().minusNanos(1);

        List<LedgerEntry> entries = ledgerEntryRepository
                .findByAccount_AccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(accountId, start, end);

        BigDecimal openingBalance = entries.isEmpty()
                ? normalizedMoney(account.getLedgerBalance())
                : normalizedMoney(entries.get(0).getBalanceBefore());
        BigDecimal closingBalance = entries.isEmpty()
                ? openingBalance
                : normalizedMoney(entries.get(entries.size() - 1).getBalanceAfter());
        BigDecimal totalDebit = entries.stream()
                .filter(entry -> entry.getEntryType() == EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .map(this::normalizedMoney)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal totalCredit = entries.stream()
                .filter(entry -> entry.getEntryType() == EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .map(this::normalizedMoney)
                .reduce(ZERO, BigDecimal::add);

        DailyBalanceSnapshot snapshot = dailyBalanceSnapshotRepository
                .findByAccount_AccountIdAndSnapshotDate(accountId, targetDate)
                .orElse(DailyBalanceSnapshot.builder().account(account).snapshotDate(targetDate).build());
        snapshot.setOpeningBalance(openingBalance);
        snapshot.setClosingBalance(closingBalance);
        snapshot.setTotalDebit(totalDebit);
        snapshot.setTotalCredit(totalCredit);
        DailyBalanceSnapshot savedSnapshot = dailyBalanceSnapshotRepository.save(snapshot);

        AccountSnapshotCreatedEvent snapshotCreatedEvent = AccountSnapshotCreatedEvent.builder()
                .snapshotId(savedSnapshot.getSnapshotId())
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .snapshotDate(savedSnapshot.getSnapshotDate())
                .openingBalance(normalizedMoney(savedSnapshot.getOpeningBalance()))
                .closingBalance(normalizedMoney(savedSnapshot.getClosingBalance()))
                .totalDebit(normalizedMoney(savedSnapshot.getTotalDebit()))
                .totalCredit(normalizedMoney(savedSnapshot.getTotalCredit()))
                .currency(account.getCurrency())
                .createdAt(LocalDateTime.now())
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String key = account.getAccountId() + "_" + savedSnapshot.getSnapshotDate();
                kafka.send("account-snapshot-created", key, snapshotCreatedEvent);
            }
        });

        return toSnapshotResponse(savedSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SnapshotResponse> getDailySnapshots(Long accountId, LocalDate from, LocalDate to) {
        LocalDate actualFrom = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate actualTo = to == null ? LocalDate.now() : to;
        return dailyBalanceSnapshotRepository
                .findByAccount_AccountIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(accountId, actualFrom, actualTo)
                .stream()
                .map(this::toSnapshotResponse)
                .toList();
    }

    private void validateCreateAccountRequest(CreateAccountRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create account request is required");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getAccountType() == null) {
            throw new IllegalArgumentException("accountType is required");
        }
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        if (request.getInitialBalance() != null && request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("initialBalance cannot be negative");
        }
    }

    private void validateUserEligibility(Long userId) {
        UserAccountEligibilityResponse eligibility = userServiceClient.getAccountEligibility(userId);
        if (eligibility == null || !eligibility.isUserExists()) {
            throw new IllegalArgumentException("User not found in User-Service");
        }
        if (!eligibility.isActive()) {
            throw new IllegalStateException("User account is not active");
        }
        if (!eligibility.isKycVerified()) {
            throw new IllegalStateException("User KYC must be verified before opening account");
        }
        if (!eligibility.isEligible()) {
            throw new IllegalStateException(blankToDefault(eligibility.getReason(), "User is not eligible to create account"));
        }
    }

    private void validateJournalRequest(CreateJournalRequest request) {
        if (request == null || request.getEntries() == null || request.getEntries().size() < 2) {
            throw new IllegalArgumentException("Journal must contain at least two entries");
        }
        for (LedgerLineRequest line : request.getEntries()) {
            if (line.getAccountId() == null || line.getEntryType() == null || line.getAmount() == null) {
                throw new IllegalArgumentException("Each journal entry must contain accountId, entryType and amount");
            }
        }
    }

    private void ensureAccountCanPost(Account account, EntryType entryType) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Closed account cannot receive ledger entries");
        }
        if (account.getStatus() == AccountStatus.FROZEN && entryType == EntryType.DEBIT) {
            throw new IllegalStateException("Frozen account cannot be debited");
        }
    }

    private void saveStatusHistory(
            Account account,
            AccountStatus oldStatus,
            AccountStatus newStatus,
            String reason,
            String changedBy
    ) {
        statusHistoryRepository.save(AccountStatusHistory.builder()
                .account(account)
                .oldStatus(oldStatus == null ? null : oldStatus.name())
                .newStatus(newStatus.name())
                .reason(reason)
                .changedBy(blankToDefault(changedBy, "SYSTEM"))
                .changedAt(LocalDateTime.now())
                .build());
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    private String generateAccountNumber(Long userId) {
        String seed = String.valueOf(Math.abs(Objects.hash(userId, UUID.randomUUID().toString())));
        String suffix = seed.length() > 12 ? seed.substring(0, 12) : String.format("%012d", Long.parseLong(seed));
        return "10" + suffix;
    }

    private BigDecimal normalizedMoney(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private ApiResponse success(String message) {
        return ApiResponse.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage(message)
                .build();
    }

    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .currency(account.getCurrency())
                .availableBalance(normalizedMoney(account.getAvailableBalance()))
                .ledgerBalance(normalizedMoney(account.getLedgerBalance()))
                .minimumBalance(normalizedMoney(account.getMinimumBalance()))
                .openedAt(account.getOpenedAt())
                .closedAt(account.getClosedAt())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private StatusHistoryResponse toStatusHistoryResponse(AccountStatusHistory history) {
        return StatusHistoryResponse.builder()
                .id(history.getId())
                .accountId(history.getAccount().getAccountId())
                .oldStatus(history.getOldStatus())
                .newStatus(history.getNewStatus())
                .reason(history.getReason())
                .changedBy(history.getChangedBy())
                .changedAt(history.getChangedAt())
                .build();
    }

    private LedgerEntryResponse toLedgerEntryResponse(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .entryId(entry.getEntryId())
                .journalId(entry.getJournal().getJournalId())
                .accountId(entry.getAccount().getAccountId())
                .accountNumber(entry.getAccount().getAccountNumber())
                .entryType(entry.getEntryType())
                .amount(normalizedMoney(entry.getAmount()))
                .currency(entry.getCurrency())
                .balanceBefore(normalizedMoney(entry.getBalanceBefore()))
                .balanceAfter(normalizedMoney(entry.getBalanceAfter()))
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private LedgerJournalResponse toLedgerJournalResponse(LedgerJournal journal) {
        return LedgerJournalResponse.builder()
                .journalId(journal.getJournalId())
                .referenceType(journal.getReferenceType())
                .referenceId(journal.getReferenceId())
                .description(journal.getDescription())
                .status(journal.getStatus())
                .createdBy(journal.getCreatedBy())
                .createdAt(journal.getCreatedAt())
                .postedAt(journal.getPostedAt())
                .entries(journal.getEntries() == null ? List.of() : journal.getEntries().stream().map(this::toLedgerEntryResponse).toList())
                .build();
    }

    private AccountHoldResponse toAccountHoldResponse(AccountHold hold) {
        return AccountHoldResponse.builder()
                .holdId(hold.getHoldId())
                .accountId(hold.getAccount().getAccountId())
                .amount(normalizedMoney(hold.getAmount()))
                .reason(hold.getReason())
                .referenceId(hold.getReferenceId())
                .status(hold.getStatus())
                .expiredAt(hold.getExpiredAt())
                .createdAt(hold.getCreatedAt())
                .build();
    }

    private SnapshotResponse toSnapshotResponse(DailyBalanceSnapshot snapshot) {
        return SnapshotResponse.builder()
                .snapshotId(snapshot.getSnapshotId())
                .accountId(snapshot.getAccount().getAccountId())
                .snapshotDate(snapshot.getSnapshotDate())
                .openingBalance(normalizedMoney(snapshot.getOpeningBalance()))
                .closingBalance(normalizedMoney(snapshot.getClosingBalance()))
                .totalDebit(normalizedMoney(snapshot.getTotalDebit()))
                .totalCredit(normalizedMoney(snapshot.getTotalCredit()))
                .build();
    }
}
