package com.example.Model.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BroadcastNotificationRequest {
    private Set<Long> userIds;
    private String topic;

    // Content
    private String type;
    private String title;
    private String body;
    // Audit
    private String createdBy;
}
