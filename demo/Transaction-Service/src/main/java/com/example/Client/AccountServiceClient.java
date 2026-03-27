package com.example.Client;

import com.example.Client.Dto.AccountEligibilityResponse;
import com.example.Config.Feign.FeignClientConfig;
import com.example.Config.Feign.FeignRetryConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "account-service",
        url = "${services.account-service.url:http://localhost:8085}",
        configuration = { FeignRetryConfig.class,
                FeignClientConfig.class}
)
public interface AccountServiceClient {
//    @GetMapping("/api/users/internal/{userId}/account-eligibility")
//    AccountEligibilityResponse getAccountEligibility(@PathVariable("userId") Long userId);
}

