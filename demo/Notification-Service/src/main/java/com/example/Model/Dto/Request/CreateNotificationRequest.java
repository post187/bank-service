package com.example.Model.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateNotificationRequest {
    private Long userId;
    private String recipientEmail;
    private String type;
    private String title;
    private String message;
    private Map<String, Object> metadata;
}
