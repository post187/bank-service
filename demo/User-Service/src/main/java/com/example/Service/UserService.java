package com.example.Service;

import com.example.Model.Dto.Response.AccountEligibilityResponse;

public interface UserService extends UserAuth, UserAdminService, UserProfileService, KycService {
    AccountEligibilityResponse getAccountEligibility(Long userId);
}
