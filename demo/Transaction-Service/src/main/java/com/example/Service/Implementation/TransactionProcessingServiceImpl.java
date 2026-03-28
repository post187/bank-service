package com.example.Service.Implementation;

import com.example.Client.AccountServiceClient;
import com.example.Client.Dto.account.AccountResponse;
import com.example.Client.Dto.account.CreateJournalRequest;
import com.example.Client.Dto.account.EntryType;
import com.example.Client.Dto.account.LedgerJournalResponse;
import com.example.Client.Dto.account.LedgerLineRequest;
import com.example.Client.Dto.account.ReferenceType;
import com.example.Client.UserServiceClient;
import com.example.Client.Dto.user.UserInfoDto;
import com.example.Exception.ConcurrentIdempotencyException;
import com.example.Model.Dto.External.TransactionCompletedEvent;
import com.example.Model.Dto.Request.DepositRequest;
import com.example.Model.Dto.Request.TransferRequest;
import com.example.Model.Dto.Request.WithdrawRequest;
import com.example.Model.Dto.Response.TransactionResponse;
import com.example.Model.Entity.MonetaryTransaction;
import com.example.Model.Enum.TransactionKind;
import com.example.Model.Enum.TxnWorkflowStatus;
import com.example.Repository.MonetaryTransactionRepository;
import com.example.Service.TransactionProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransactionProcessingServiceImpl implements TransactionProcessingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final MonetaryTransactionRepository monetaryTransactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final UserServiceClient userServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${transaction.clearing-account-id}")
    private Long clearingAccountId;

    @Value("${transaction.daily-outbound-limit-per-user}")
    private BigDecimal dailyOutboundLimitPerUser;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse deposit(DepositRequest request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        return monetaryTransactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toResponse)
                .orElseGet(() -> {
                    try {
                        return insertAndPostDeposit(request, idempotencyKey);
                    } catch (ConcurrentIdempotencyException e) {
                        return toResponse(e.getExisting());
                    }
                });
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse withdraw(WithdrawRequest request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        return monetaryTransactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toResponse)
                .orElseGet(() -> {
                    try {
                        return insertAndPostWithdraw(request, idempotencyKey);
                    } catch (ConcurrentIdempotencyException e) {
                        return toResponse(e.getExisting());
                    }
                });
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse transfer(TransferRequest request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        return monetaryTransactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toResponse)
                .orElseGet(() -> {
                    try {
                        return insertAndPostTransfer(request, idempotencyKey);
                    } catch (ConcurrentIdempotencyException e) {
                        return toResponse(e.getExisting());
                    }
                });
    }

    private TransactionResponse insertAndPostDeposit(DepositRequest request, String idempotencyKey) {
        UserInfoDto user = userServiceClient.getMyInfo();
        Long userId = requireUserId(user);
        AccountResponse to = accountServiceClient.getAccount(request.getToAccountId());
        assertAccountOwnedByUser(to, userId);

        BigDecimal amount = normalizeAmount(request.getAmount());
        String currency = resolveCurrency(request.getCurrency(), to);
        assertCurrencyMatchesAccount(to, currency);

        MonetaryTransaction tx = MonetaryTransaction.builder()
                .idempotencyKey(idempotencyKey)
                .kind(TransactionKind.DEPOSIT)
                .status(TxnWorkflowStatus.PENDING)
                .amount(amount)
                .currency(currency)
                .fromAccountId(null)
                .toAccountId(to.getAccountId())
                .initiatorUserId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        tx = persistNewOrReturnExistingSnapshot(tx, idempotencyKey);

        CreateJournalRequest journal = CreateJournalRequest.builder()
                .referenceType(ReferenceType.DEPOSIT)
                .referenceId(tx.getId())
                .description("Deposit (transaction-service)")
                .createdBy("TRANSACTION_SERVICE")
                .entries(List.of(
                        LedgerLineRequest.builder()
                                .accountId(clearingAccountId)
                                .entryType(EntryType.DEBIT)
                                .amount(amount)
                                .currency(currency)
                                .build(),
                        LedgerLineRequest.builder()
                                .accountId(to.getAccountId())
                                .entryType(EntryType.CREDIT)
                                .amount(amount)
                                .currency(currency)
                                .build()
                ))
                .build();

        return finalizeWithJournal(tx, journal);
    }



    private TransactionResponse insertAndPostWithdraw(WithdrawRequest request, String idempotencyKey) {
        UserInfoDto user = userServiceClient.getMyInfo();
        Long userId = requireUserId(user);
        AccountResponse from = accountServiceClient.getAccount(request.getFromAccountId());
        assertAccountOwnedByUser(from, userId);

        BigDecimal amount = normalizeAmount(request.getAmount());
        String currency = resolveCurrency(request.getCurrency(), from);
        assertCurrencyMatchesAccount(from, currency);
        assertDailyOutboundLimit(userId, amount);

        MonetaryTransaction tx = MonetaryTransaction.builder()
                .idempotencyKey(idempotencyKey)
                .kind(TransactionKind.WITHDRAW)
                .status(TxnWorkflowStatus.PENDING)
                .amount(amount)
                .currency(currency)
                .fromAccountId(from.getAccountId())
                .toAccountId(null)
                .initiatorUserId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        tx = persistNewOrReturnExistingSnapshot(tx, idempotencyKey);

        CreateJournalRequest journal = CreateJournalRequest.builder()
                .referenceType(ReferenceType.WITHDRAW)
                .referenceId(tx.getId())
                .description("Withdraw (transaction-service)")
                .createdBy("TRANSACTION_SERVICE")
                .entries(List.of(
                        LedgerLineRequest.builder()
                                .accountId(from.getAccountId())
                                .entryType(EntryType.DEBIT)
                                .amount(amount)
                                .currency(currency)
                                .build(),
                        LedgerLineRequest.builder()
                                .accountId(clearingAccountId)
                                .entryType(EntryType.CREDIT)
                                .amount(amount)
                                .currency(currency)
                                .build()
                ))
                .build();

        return finalizeWithJournal(tx, journal);
    }

    private TransactionResponse insertAndPostTransfer(TransferRequest request, String idempotencyKey) {
        UserInfoDto user = userServiceClient.getMyInfo();
        Long userId = requireUserId(user);
        if (Objects.equals(request.getFromAccountId(), request.getToAccountId())) {
            throw new IllegalArgumentException("fromAccountId and toAccountId must differ");
        }

        AccountResponse from = accountServiceClient.getAccount(request.getFromAccountId());
        AccountResponse to = accountServiceClient.getAccount(request.getToAccountId());
        assertAccountOwnedByUser(from, userId);

        BigDecimal amount = normalizeAmount(request.getAmount());
        String currency = resolveCurrency(request.getCurrency(), from);
        assertCurrencyMatchesAccount(from, currency);
        if (!from.getCurrency().equalsIgnoreCase(to.getCurrency())) {
            throw new IllegalArgumentException("Cross-currency transfer is not supported");
        }
        assertDailyOutboundLimit(userId, amount);

        MonetaryTransaction tx = MonetaryTransaction.builder()
                .idempotencyKey(idempotencyKey)
                .kind(TransactionKind.INTERNAL_TRANSFER)
                .status(TxnWorkflowStatus.PENDING)
                .amount(amount)
                .currency(currency)
                .fromAccountId(from.getAccountId())
                .toAccountId(to.getAccountId())
                .initiatorUserId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        tx = persistNewOrReturnExistingSnapshot(tx, idempotencyKey);

        CreateJournalRequest journal = CreateJournalRequest.builder()
                .referenceType(ReferenceType.TRANSFER)
                .referenceId(tx.getId())
                .description("Internal transfer (transaction-service)")
                .createdBy("TRANSACTION_SERVICE")
                .entries(List.of(
                        LedgerLineRequest.builder()
                                .accountId(from.getAccountId())
                                .entryType(EntryType.DEBIT)
                                .amount(amount)
                                .currency(currency)
                                .build(),
                        LedgerLineRequest.builder()
                                .accountId(to.getAccountId())
                                .entryType(EntryType.CREDIT)
                                .amount(amount)
                                .currency(currency)
                                .build()
                ))
                .build();

        return finalizeWithJournal(tx, journal);
    }

    private MonetaryTransaction persistNewOrReturnExistingSnapshot(MonetaryTransaction tx, String idempotencyKey) {
        try {
            MonetaryTransaction saved = monetaryTransactionRepository.save(tx);
            monetaryTransactionRepository.flush();
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw new ConcurrentIdempotencyException(
                    monetaryTransactionRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> ex)
            );
        }
    }

    private TransactionResponse finalizeWithJournal(MonetaryTransaction tx, CreateJournalRequest journal) {
        if (tx.getStatus() == TxnWorkflowStatus.POSTED) {
            return toResponse(tx);
        }
        try {
            LedgerJournalResponse response = accountServiceClient.postJournal(journal);
            tx.setStatus(TxnWorkflowStatus.POSTED);
            tx.setLedgerJournalId(response.getJournalId());
            tx.setCompletedAt(LocalDateTime.now());
            tx.setFailureReason(null);
            MonetaryTransaction saved = monetaryTransactionRepository.save(tx);
            registerKafkaAfterCommit(saved);
            return toResponse(saved);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            tx.setStatus(TxnWorkflowStatus.FAILED);
            tx.setFailureReason(truncate(msg, 1900));
            monetaryTransactionRepository.save(tx);
            throw new IllegalStateException("Ledger post failed: " + msg, e);
        }
    }

    private void registerKafkaAfterCommit(MonetaryTransaction tx) {
        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(tx.getId())
                .kind(tx.getKind())
                .initiatorUserId(tx.getInitiatorUserId())
                .ledgerJournalId(tx.getLedgerJournalId())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .fromAccountId(tx.getFromAccountId())
                .toAccountId(tx.getToAccountId())
                .completedAt(tx.getCompletedAt())
                .build();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send("transaction-completed", tx.getId().toString(), event);
            }
        });
    }

    private void assertDailyOutboundLimit(Long userId, BigDecimal additionalAmount) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        BigDecimal used = monetaryTransactionRepository.sumCompletedOutbound(
                userId,
                TxnWorkflowStatus.POSTED,
                List.of(TransactionKind.WITHDRAW, TransactionKind.INTERNAL_TRANSFER),
                start,
                end
        );
        BigDecimal normalizedUsed = normalizeAmount(used);
        BigDecimal limit = normalizeAmount(dailyOutboundLimitPerUser);
        if (normalizedUsed.add(additionalAmount).compareTo(limit) > 0) {
            throw new IllegalStateException("Daily outbound transaction limit exceeded");
        }
    }

    private static void validateIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey) || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key header is required (max 128 chars)");
        }
    }

    private static Long requireUserId(UserInfoDto user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalStateException("Cannot resolve user from token");
        }
        return user.getUserId();
    }

    private static void assertAccountOwnedByUser(AccountResponse account, Long userId) {
        if (account.getUserId() == null || !account.getUserId().equals(userId)) {
            throw new IllegalStateException("Account does not belong to the authenticated user");
        }
    }

    private static void assertCurrencyMatchesAccount(AccountResponse account, String currency) {
        if (!account.getCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Currency does not match account currency");
        }
    }

    private static String resolveCurrency(String requested, AccountResponse account) {
        if (StringUtils.hasText(requested)) {
            return requested.trim().toUpperCase();
        }
        return account.getCurrency().trim().toUpperCase();
    }

    private static BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private TransactionResponse toResponse(MonetaryTransaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .idempotencyKey(tx.getIdempotencyKey())
                .kind(tx.getKind())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .fromAccountId(tx.getFromAccountId())
                .toAccountId(tx.getToAccountId())
                .initiatorUserId(tx.getInitiatorUserId())
                .ledgerJournalId(tx.getLedgerJournalId())
                .failureReason(tx.getFailureReason())
                .createdAt(tx.getCreatedAt())
                .completedAt(tx.getCompletedAt())
                .build();
    }
}
