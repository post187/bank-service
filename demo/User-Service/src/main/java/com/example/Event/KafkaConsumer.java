package com.example.Event;

import com.example.Model.Dto.External.KycAiResultEvent;
import com.example.Service.KycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.example.Constant.AppConstant.GROUP_ID;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumer {
    private final KycService kycService;

    @KafkaListener(topics = "kyc-ai-result", groupId = GROUP_ID)
    public void handleKycAiResult(KycAiResultEvent event) {
        kycService.processAiResult(event);
    }
}
