package com.example.Model.Entity;

import com.example.Model.Dto.Internal.Status.DocumentType;
import com.example.Model.Dto.Internal.Status.KycStatus;
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
@Table(name = "user-kyc-documents")
public class UserKycDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String identificationNumber;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private String documentUrl;

    private String selfieUrl;

    @Enumerated(EnumType.STRING)
    private KycStatus status;

    private String rejectionReason;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private String reviewedBy;
}
