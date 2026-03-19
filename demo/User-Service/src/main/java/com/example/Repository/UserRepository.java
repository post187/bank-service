package com.example.Repository;

import com.example.Model.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.authId = :authId")
    Optional<User> findUserByAuthId(String authId);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
