package com.example.Service.Impl;

import com.example.Client.UserInternalClient;
import com.example.Model.Document.Notification;
import com.example.Model.Dto.Request.BroadcastNotificationRequest;
import com.example.Model.Dto.Request.CreateNotificationRequest;
import com.example.Model.Dto.Response.NotificationResponse;
import com.example.Model.Dto.Response.Response;
import com.example.Repository.NotificationRepository;
import com.example.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserInternalClient userInternalClient;

    @Override
    public Response createNotification(CreateNotificationRequest request) {
        String email = StringUtils.hasText(request.getRecipientEmail())
                ? request.getRecipientEmail().trim()
                : resolveEmail(request.getUserId());
        Notification n = Notification.builder()
                .userId(request.getUserId())
                .recipientEmail(email)
                .title(request.getTitle())
                .body(request.getMessage())
                .category(request.getType())
                .sourceService("API")
                .sourceTopic("manual")
                .kafkaMessageKey(null)
                .payload(request.getMetadata())
                .read(false)
                .emailDeliveryStatus("SKIPPED")
                .build();
        notificationRepository.save(n);
        return Response.builder()
                .responseCode("200")
                .responseMessage("Notification created")
                .build();
    }

    @Override
    public Response createBroadcastNotification(BroadcastNotificationRequest request) {
        if (request.getUserIds() == null) {
            return Response.builder()
                    .responseMessage("No user")
                    .responseCode("409")
                    .build();
        }
        if (request.getBody() == null) {
            return Response.builder()
                    .responseMessage("No user")
                    .responseCode("409")
                    .build();
        }
        String email = userInternalClient.getMyInfo().getEmail();
        Long userId = userInternalClient.getMyInfo().getUserId();
        Notification newNotification = Notification.builder()
                .userId(userId)
                .recipientEmail(email)
                .body(request.getBody())
                .title(request.getTitle())
                .sourceTopic(request.getTopic())
                .category("Broadcast")
                .payload(null)
                .emailDispatched(true)
                .emailDeliveryStatus("")
                .build();

        notificationRepository.save(newNotification);

        return Response.builder()
                .responseCode("200")
                .responseMessage("Broadcast notification successfully.")
                .build();
    }

    @Override
    public Page<NotificationResponse> getNotificationsByEmail(String email, int page, int size) {
        return notificationRepository
                .findByRecipientEmailOrderByCreatedAtDesc(email, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Override
    public void markAsReadByEmail(String email, String notificationId) {
        notificationRepository.findByIdAndRecipientEmail(notificationId, email).ifPresent(n -> {
            n.setRead(true);
            n.setReadAt(System.currentTimeMillis());
            notificationRepository.save(n);
        });
    }

    @Override
    public void markAllAsReadByEmail(String email) {
        int page = 0;
        Page<Notification> batch;
        do {
            batch = notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email, PageRequest.of(page++, 500));
            for (Notification n : batch.getContent()) {
                if (!n.isRead()) {
                    n.setRead(true);
                    n.setReadAt(System.currentTimeMillis());
                    notificationRepository.save(n);
                }
            }
        } while (batch.hasNext());
    }

    @Override
    public void deleteNotificationByEmail(String email, String notificationId) {
        notificationRepository.findByIdAndRecipientEmail(notificationId, email)
                .ifPresent(notificationRepository::delete);
    }

    @Override
    public NotificationResponse getNotificationDetailByEmail(String email, String notificationId) {
        return notificationRepository.findByIdAndRecipientEmail(notificationId, email)
                .map(this::toResponse)
                .orElse(null);
    }

    private String resolveEmail(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return userInternalClient.getEmail(userId).getEmail();
        } catch (Exception e) {
            return null;
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .recipientEmail(n.getRecipientEmail())
                .title(n.getTitle())
                .body(n.getBody())
                .category(n.getCategory())
                .sourceService(n.getSourceService())
                .sourceTopic(n.getSourceTopic())
                .read(n.isRead())
                .emailDispatched(n.isEmailDispatched())
                .emailDeliveryStatus(n.getEmailDeliveryStatus())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
