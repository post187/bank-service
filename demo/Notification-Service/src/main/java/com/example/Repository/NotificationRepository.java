package com.example.Repository;

import com.example.Model.Document.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);

    Optional<Notification> findByIdAndRecipientEmail(String id, String recipientEmail);

    long countByRecipientEmailAndRead(String recipientEmail, boolean read);
}
