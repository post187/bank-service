package com.example.Client;

import com.example.Client.Dto.AccountOwnerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service", url = "${services.account-service.url:http://localhost:8085}")
public interface AccountInternalClient {

    @GetMapping("/api/accounts/internal/{accountId}/owner")
    AccountOwnerResponse getOwner(@PathVariable("accountId") Long accountId);
}
