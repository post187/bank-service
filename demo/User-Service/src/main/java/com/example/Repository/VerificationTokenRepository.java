package com.example.Repository;

import com.example.Model.Entity.User;
import com.example.Model.Entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    void deleteByUser(User user);
    Optional<VerificationToken> findByToken(String token);
    boolean existsByToken(String token);

}
