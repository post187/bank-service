package com.example.Client;

import com.example.Client.Dto.UserAccountEligibilityResponse;
import com.example.Config.Flient.FeignClientConfig;
import com.example.Config.Flient.FeignRetryConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        url = "${services.user-service.url:http://localhost:8082}",
        configuration = { FeignRetryConfig.class,
            FeignClientConfig.class}
)
public interface UserServiceClient {
    @GetMapping("/api/users/internal/{userId}/account-eligibility")
    UserAccountEligibilityResponse getAccountEligibility(@PathVariable("userId") Long userId);
}
