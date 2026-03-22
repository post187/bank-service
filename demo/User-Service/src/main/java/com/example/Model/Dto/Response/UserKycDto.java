package com.example.Model.Dto.Response;

import com.example.Model.Dto.Internal.Status.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserKycDto {
    private Long kycId;

    private Long userId;

    private String identificationNumber;

    private DocumentType documentType;

    private String documentUrl;

    private String selfieUrl;

    private String rejectionReason;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private String reviewedBy;
}
