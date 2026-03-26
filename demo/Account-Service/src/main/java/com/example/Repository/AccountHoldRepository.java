package com.example.Repository;

import com.example.Model.Entity.AccountHold;
import com.example.Model.Status.AccountHoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountHoldRepository extends JpaRepository<AccountHold, Long> {
    List<AccountHold> findByAccount_AccountIdOrderByCreatedAtDesc(Long accountId);
    List<AccountHold> findByAccount_AccountIdAndStatus(Long accountId, AccountHoldStatus status);
}
