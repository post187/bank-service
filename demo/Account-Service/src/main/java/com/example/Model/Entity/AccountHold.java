package com.example.Model.Entity;

import com.example.Model.Status.AccountHoldStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "account_holds")
public class AccountHold {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long holdId;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    private BigDecimal amount;
    private String reason;
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    private AccountHoldStatus status;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;

}
