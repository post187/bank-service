package com.example.Model.Dto.Response;

import com.example.Model.Dto.Internal.StatusUserService.DocumentType;
import com.example.Model.Dto.Internal.StatusUserService.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserKycDtoUser {
    private Long kycId;

    private KycStatus status;

    private String maskedIdNumber;
    private DocumentType documentType;
    private String fullName;

    private String frontCardUrl;
    private String backCardUrl;
    private String selfieUrl;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    private String statusMessage;
}
