package com.example.Repository;

import com.example.Model.Entity.AccountStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountStatusHistoryRepository extends JpaRepository<AccountStatusHistory, Long> {
    List<AccountStatusHistory> findByAccount_AccountIdOrderByChangedAtDesc(Long accountId);
    List<AccountStatusHistory> findByAccount_AccountIdAndChangedAtBetweenOrderByChangedAtDesc(
            Long accountId,
            LocalDateTime from,
            LocalDateTime to
    );
}
