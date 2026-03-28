package com.example.Service;

import com.example.Model.Dto.Response.AccountEligibilityResponse;

public interface UserService extends UserAuth, UserAdminService, UserProfileService, KycService {
    AccountEligibilityResponse getAccountEligibility(Long userId);

    /** Dùng cho Notification-Service / mesh nội bộ (không trả JWT). */
    String getEmailByUserIdInternal(Long userId);

    Long getUserIdByEmailInternal(String email);
}
