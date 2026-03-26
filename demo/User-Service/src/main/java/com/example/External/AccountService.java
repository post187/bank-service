package com.example.External;

import com.example.Model.External.Account;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "account-service",
        url = "${services.account.url}",
        configuration = FeignClientProperties.FeignClientConfiguration.class
)
public interface AccountService {
    @GetMapping("/api/accounts/{accountId}")
    public ResponseEntity<Account> readByAccountNumber(@PathVariable Long accountNumber);
}
