package com.example.Repository;

import com.example.Model.Dto.Internal.StatusUserService.KycStatus;
import com.example.Model.Entity.UserKycDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserKycRepository extends JpaRepository<UserKycDocument, Long> {
    Page<UserKycDocument> findByStatus(KycStatus status, Pageable pageable);

    boolean existsByUser_UserIdAndStatus(Long userId, KycStatus status);

    List<UserKycDocument> findByUser_EmailOrderBySubmittedAtDesc(String email);

    Optional<UserKycDocument> findTopByUser_EmailOrderBySubmittedAtDesc(String email);

    Optional<UserKycDocument> findTopByUser_UserIdOrderBySubmittedAtDesc(Long userId);
}
