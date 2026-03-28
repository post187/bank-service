package com.example.Model.Dto.Response;

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
public class UserKycDtoAdmin {
    private Long kycId;
    private Long userId;
    private KycStatus status;

    private String identificationNumber;
    private String ocrDetectedId;    // Số ID AI đọc được từ ảnh
    private String ocrFullName;      // Tên AI đọc được
    private double faceMatchScore;   // Độ khớp khuôn mặt (Ví dụ: 98.5%)

    private String frontCardUrl;
    private String backCardUrl;
    private String selfieUrl;

    private String adminNote;
    private String rejectionReason;
    private String reviewedBy;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    private boolean isPotentiallyFake;
}
