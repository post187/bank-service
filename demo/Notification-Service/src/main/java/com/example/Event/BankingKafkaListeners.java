package com.example.Event;

import com.example.Constant.KafkaTopics;
import com.example.Service.Impl.KafkaIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BankingKafkaListeners {

    private final KafkaIngestService kafkaIngestService;

    @KafkaListener(
            topics = KafkaTopics.REGISTRATION,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRegistration(
            @Header(KafkaHeaders.RECEIVED_KEY) String email,
            @Payload String token,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onRegistration(email, token);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = KafkaTopics.RESET_PASSWORD,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onResetPassword(
            @Header(KafkaHeaders.RECEIVED_KEY) String email,
            @Payload String payload,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onResetPassword(email, payload);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = KafkaTopics.KYC_USER,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onKycUser(
            @Header(KafkaHeaders.RECEIVED_KEY) String email,
            @Payload String message,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onKycUserMessage(email, message);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = KafkaTopics.ABLE_USER,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAbleUser(
            @Header(KafkaHeaders.RECEIVED_KEY) String email,
            @Payload String message,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onAbleUser(email, message);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = KafkaTopics.VERIFY_NEW_DEVICE,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onVerifyNewDevice(
            @Payload String json,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onVerifyNewDevice(json);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = KafkaTopics.ACCOUNT_CREATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAccountCreated(
            @Header(KafkaHeaders.RECEIVED_KEY) String userIdKey,
            @Payload String message,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onAccountStringEvent(KafkaTopics.ACCOUNT_CREATED, userIdKey, message);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = KafkaTopics.ACCOUNT_STATUS_CHANGED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAccountStatus(
            @Header(KafkaHeaders.RECEIVED_KEY) String userIdKey,
            @Payload String message,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onAccountStringEvent(KafkaTopics.ACCOUNT_STATUS_CHANGED, userIdKey, message);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = KafkaTopics.ACCOUNT_CLOSED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAccountClosed(
            @Header(KafkaHeaders.RECEIVED_KEY) String userIdKey,
            @Payload String message,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onAccountStringEvent(KafkaTopics.ACCOUNT_CLOSED, userIdKey, message);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = KafkaTopics.LEDGER_JOURNAL_POSTED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onLedgerPosted(
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Payload String json,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onLedgerJournalPosted(key, json);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = KafkaTopics.HOLD_CREATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onHoldCreated(
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Payload String json,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onHoldCreated(key, json);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = KafkaTopics.HOLD_RELEASED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onHoldReleased(
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Payload String json,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onHoldReleased(key, json);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = KafkaTopics.ACCOUNT_SNAPSHOT_CREATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSnapshot(
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Payload String json,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onSnapshotCreated(key, json);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = KafkaTopics.TRANSACTION_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionCompleted(
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Payload String json,
            Acknowledgment ack
    ) {
        try {
            kafkaIngestService.onTransactionCompleted(key, json);
        } finally {
            ack.acknowledge();
        }
    }
}
