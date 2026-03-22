package com.example.Repository;

import com.example.Model.Entity.UserKycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserKycRepository extends JpaRepository<UserKycDocument, Long> {
}
