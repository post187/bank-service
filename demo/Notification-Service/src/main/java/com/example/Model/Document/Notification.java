package com.example.Model.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    /** User chủ sở hữu thông báo (nếu xác định được). */
    private Long userId;

    /** Dùng để list theo JWT subject (email). */
    @Indexed
    private String recipientEmail;

    private String title;
    private String body;

    /** REGISTRATION, SECURITY, KYC, ACCOUNT, LEDGER, HOLD, SNAPSHOT, TRANSACTION, BILLING */
    private String category;

    private String sourceService;
    private String sourceTopic;
    private String kafkaMessageKey;

    private Map<String, Object> payload;

    @Field("isRead")
    @Builder.Default
    private boolean read = false;

    private Long readAt;

    @Builder.Default
    private boolean emailDispatched = false;

    /** PENDING, SENT, FAILED, SKIPPED */
    private String emailDeliveryStatus;

    private Long relatedEmailLogId;

    @Builder.Default
    private long createdAt = System.currentTimeMillis();
}
