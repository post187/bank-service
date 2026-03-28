package com.example.Repository;

import com.example.Model.Entity.MonetaryTransaction;
import com.example.Model.Enum.TransactionKind;
import com.example.Model.Enum.TxnWorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface MonetaryTransactionRepository extends JpaRepository<MonetaryTransaction, Long> {

    Optional<MonetaryTransaction> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM MonetaryTransaction t
            WHERE t.initiatorUserId = :userId
              AND t.status = :status
              AND t.kind IN :kinds
              AND t.completedAt >= :from
              AND t.completedAt < :to
            """)
    BigDecimal sumCompletedOutbound(
            @Param("userId") Long userId,
            @Param("status") TxnWorkflowStatus status,
            @Param("kinds") Collection<TransactionKind> kinds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
