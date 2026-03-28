package com.example.Service;

import com.example.Model.Dto.Request.BroadcastNotificationRequest;
import com.example.Model.Dto.Request.CreateNotificationRequest;
import com.example.Model.Dto.Response.NotificationResponse;
import com.example.Model.Dto.Response.Response;
import org.springframework.data.domain.Page;

public interface NotificationService {

    Response createNotification(CreateNotificationRequest request);

    Response createBroadcastNotification(BroadcastNotificationRequest request);

    Page<NotificationResponse> getNotificationsByEmail(String email, int page, int size);

    void markAsReadByEmail(String email, String notificationId);

    void markAllAsReadByEmail(String email);

    void deleteNotificationByEmail(String email, String notificationId);

    NotificationResponse getNotificationDetailByEmail(String email, String notificationId);
}
