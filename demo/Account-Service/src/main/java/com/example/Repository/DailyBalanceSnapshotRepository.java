package com.example.Repository;

import com.example.Model.Entity.DailyBalanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyBalanceSnapshotRepository extends JpaRepository<DailyBalanceSnapshot, Long> {
    Optional<DailyBalanceSnapshot> findByAccount_AccountIdAndSnapshotDate(Long accountId, LocalDate snapshotDate);

    List<DailyBalanceSnapshot> findByAccount_AccountIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
            Long accountId,
            LocalDate from,
            LocalDate to
    );
}
