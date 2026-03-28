package com.example.Client;

import com.example.Client.Dto.UserEmailResponse;
import com.example.Client.Dto.UserIdResponse;
import com.example.Client.Dto.UserInfo;
import com.example.Config.FeignRetryConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url = "${services.user-service.url:http://localhost:8082}", configuration = FeignRetryConfig.class)
public interface UserInternalClient {

    @GetMapping("/api/users/internal/{userId}/email")
    UserEmailResponse getEmail(@PathVariable("userId") Long userId);

    @GetMapping("/api/users/internal/by-email")
    UserIdResponse getUserIdByEmail(@RequestParam("email") String email);

    @GetMapping("/api/users/my-info")
    UserInfo getMyInfo();
}
