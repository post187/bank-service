package com.example.External;

import com.example.Model.External.Account;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

//@FeignClient(name = "account-service", configuration = FeignClientProperties.FeignClientConfiguration.class)
public interface AccountService {
    @GetMapping("/accounts")
    public ResponseEntity<Account> readByAccountNumber(@RequestParam String accountNumber);
}
