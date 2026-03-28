package com.example.Client;

import com.example.Client.Dto.account.AccountResponse;
import com.example.Client.Dto.account.CreateJournalRequest;
import com.example.Client.Dto.account.LedgerJournalResponse;
import com.example.Config.Feign.FeignClientConfig;
import com.example.Config.Feign.FeignForwardAuthConfig;
import com.example.Config.Feign.FeignRetryConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "account-service",
        url = "${services.account-service.url:http://localhost:8085}",
        configuration = {FeignRetryConfig.class, FeignClientConfig.class, FeignForwardAuthConfig.class}
)
public interface AccountServiceClient {
    @GetMapping("/api/accounts/{accountId}")
    AccountResponse getAccount(@PathVariable("accountId") Long accountId);

    @PostMapping("/api/accounts/journals")
    LedgerJournalResponse postJournal(@RequestBody CreateJournalRequest request);
}
