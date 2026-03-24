package com.example.Model.Mapper;

import com.example.Model.Dto.Internal.Status.KycStatus;
import com.example.Model.Dto.Response.UserKycDtoAdmin;
import com.example.Model.Dto.Response.UserKycDtoUser;
import com.example.Model.Entity.UserKycDocument;
import org.springframework.stereotype.Component;

@Component
public class UserKycMapper {
    public UserKycDtoAdmin toDto(UserKycDocument userKyc) {
        if (userKyc == null) {
            return null;
        }
        return UserKycDtoAdmin.builder()
                .kycId(userKyc.getId())
                .userId(userKyc.getUser() != null ? userKyc.getUser().getUserId() : null)
                .status(userKyc.getStatus())
                .identificationNumber(userKyc.getSubmittedIdentificationNumber())
                .ocrDetectedId(userKyc.getOcrIdNumber())
                .ocrFullName(userKyc.getOcrFullName())
                .faceMatchScore(userKyc.getFaceMatchScore())
                .frontCardUrl(userKyc.getFrontCardUrl())
                .backCardUrl(userKyc.getBackCardUrl())
                .selfieUrl(userKyc.getSelfieUrl())
                .adminNote(userKyc.getAdminNote())
                .rejectionReason(userKyc.getRejectionReason())
                .reviewedBy(userKyc.getReviewedBy())
                .submittedAt(userKyc.getSubmittedAt())
                .reviewedAt(userKyc.getReviewedAt())
                .isPotentiallyFake(userKyc.isPotentiallyFake())
                .build();
    }

    public UserKycDtoUser toUserDto(UserKycDocument userKyc) {
        if (userKyc == null) {
            return null;
        }
        KycStatus status = userKyc.getStatus();
        return UserKycDtoUser.builder()
                .kycId(userKyc.getId())
                .status(status)
                .maskedIdNumber(maskIdNumber(userKyc.getSubmittedIdentificationNumber()))
                .documentType(userKyc.getDocumentType())
                .fullName(userKyc.getSubmittedFullName())
                .frontCardUrl(userKyc.getFrontCardUrl())
                .backCardUrl(userKyc.getBackCardUrl())
                .selfieUrl(userKyc.getSelfieUrl())
                .submittedAt(userKyc.getSubmittedAt())
                .updatedAt(userKyc.getReviewedAt() != null ? userKyc.getReviewedAt() : userKyc.getSubmittedAt())
                .statusMessage(buildStatusMessage(status, userKyc.getRejectionReason()))
                .build();
    }

    private String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.isBlank()) {
            return null;
        }
        String trimmed = idNumber.trim();
        int len = trimmed.length();
        if (len <= 4) {
            return "*".repeat(len);
        }
        String last4 = trimmed.substring(len - 4);
        return "*".repeat(len - 4) + last4;
    }
    private String buildStatusMessage(KycStatus status, String rejectionReason) {
        if (status == null) {
            return "KYC status is unavailable.";
        }
        return switch (status) {
            case NOT_SUBMITTED -> "You have not submitted KYC yet.";
            case PENDING -> "Your KYC is under review.";
            case VERIFIED -> "Your KYC has been verified successfully.";
            case REJECTED -> {
                if (rejectionReason != null && !rejectionReason.isBlank()) {
                    yield "Your KYC was rejected. Reason: " + rejectionReason;
                }
                yield "Your KYC was rejected. Please resubmit valid documents.";
            }
            case EXPIRED -> "Your KYC has expired. Please update your documents.";
            case UPDATING -> "Your KYC update is being processed.";
        };
    }
}