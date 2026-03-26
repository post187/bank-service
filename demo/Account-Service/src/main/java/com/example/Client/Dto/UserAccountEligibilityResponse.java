package com.example.Client.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAccountEligibilityResponse {
    private Long userId;
    private boolean userExists;
    private boolean active;
    private boolean kycVerified;
    private boolean eligible;
    private String latestKycStatus;
    private String reason;
}
