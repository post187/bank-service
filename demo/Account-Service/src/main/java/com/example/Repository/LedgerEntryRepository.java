package com.example.Repository;

import com.example.Model.Entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByAccount_AccountIdOrderByCreatedAtDesc(Long accountId);
    List<LedgerEntry> findByAccount_AccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long accountId,
            LocalDateTime from,
            LocalDateTime to
    );
    List<LedgerEntry> findByAccount_AccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long accountId,
            LocalDateTime from,
            LocalDateTime to
    );
}
