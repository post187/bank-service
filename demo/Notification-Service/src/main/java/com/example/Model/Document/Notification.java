package com.example.Model.Document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Builder
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private String userId;
    private String title;
    private String content;
    private String type; // ORDER, VERIFY,..
    private boolean isRead = false;

    private Map<String, Object> payload;
    private long createdAt  = System.currentTimeMillis();
}
