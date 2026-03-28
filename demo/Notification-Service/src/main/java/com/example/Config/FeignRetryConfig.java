package com.example.Config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignRetryConfig {
    @Bean
    public Retryer retryer() {
        // period, maxPeriod, maxAttempts
        return new Retryer.Default(
                1000,   // delay ban đầu (1s)
                2000,   // delay tối đa giữa các lần retry (2s)
                3       // số lần thử (tổng cộng 3 lần gọi)
        );
    }
}

