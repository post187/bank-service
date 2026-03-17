package com.example.Model.Document;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "emails")
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String notificationId;

    private String recipient;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private String status;
    private String retryCount;

    private String errorMessage;

    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
