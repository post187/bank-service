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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String submittedIdentificationNumber;
    private String submittedFullName;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private String ocrIdNumber;
    private String ocrFullName;
    private Double faceMatchScore;

    private String frontCardUrl;
    private String backCardUrl;

    private String selfieUrl;

    @Enumerated(EnumType.STRING)
    private KycStatus status;

    @Column(length = 500)
    private String rejectionReason;

    @Column(length = 1000)
    private String adminNote;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private String reviewedBy;

    private boolean isPotentiallyFake;

}
