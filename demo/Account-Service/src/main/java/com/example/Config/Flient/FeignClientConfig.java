package com.example.Config.Flient;

import feign.codec.ErrorDecoder;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig extends  FeignClientProperties.FeignClientConfiguration {
    /**
     * Returns as ErrorDecoder instance for the Feign client.
     *
     * @return the ErrorDecoder instance
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignClientErrorDecoder();
    }
}
