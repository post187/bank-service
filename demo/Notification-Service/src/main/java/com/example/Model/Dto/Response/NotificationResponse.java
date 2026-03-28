package com.example.Model.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private String id;
    private Long userId;
    private String recipientEmail;
    private String title;
    private String body;
    private String category;
    private String sourceService;
    private String sourceTopic;
    private boolean read;
    private boolean emailDispatched;
    private String emailDeliveryStatus;
    private long createdAt;
}
