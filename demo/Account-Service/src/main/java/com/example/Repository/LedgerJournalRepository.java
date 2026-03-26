package com.example.Repository;

import com.example.Model.Entity.LedgerJournal;
import com.example.Model.Status.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LedgerJournalRepository extends JpaRepository<LedgerJournal, Long> {
    Optional<LedgerJournal> findByReferenceTypeAndReferenceId(ReferenceType referenceType, Long referenceId);
    List<LedgerJournal> findDistinctByEntries_Account_AccountIdOrderByCreatedAtDesc(Long accountId);
}
