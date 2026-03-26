package com.example.Model.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "daily_balance_snapshots")
public class DailyBalanceSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long snapshotId;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    private LocalDate snapshotDate;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
}
