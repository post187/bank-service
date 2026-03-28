package com.example.Client;

import com.example.Client.Dto.user.UserInfoDto;
import com.example.Config.Feign.FeignClientConfig;
import com.example.Config.Feign.FeignForwardAuthConfig;
import com.example.Config.Feign.FeignRetryConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "user-service",
        url = "${services.user-service.url:http://localhost:8082}",
        configuration = {FeignRetryConfig.class, FeignClientConfig.class, FeignForwardAuthConfig.class}
)
public interface UserServiceClient {

    @GetMapping("/api/users/my-info")
    UserInfoDto getMyInfo();
}
