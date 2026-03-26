package com.example.Model.Entity;

import com.example.Model.Status.AccountStatus;
import com.example.Model.Status.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    private String currency;

    private BigDecimal availableBalance;
    private BigDecimal ledgerBalance;

    private BigDecimal minimumBalance;

    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<LedgerEntry> ledgerEntries;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<AccountStatusHistory> statusHistories;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<AccountHold> holds;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<DailyBalanceSnapshot> dailySnapshots;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.openedAt == null) {
            this.openedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
