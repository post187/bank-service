package com.example.Controller;

import com.example.Model.Dto.Response.NotificationResponse;
import com.example.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<NotificationResponse> myNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationService.getNotificationsByEmail(jwt.getSubject(), page, size);
    }

    @GetMapping("/me/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<NotificationResponse> detail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") String id
    ) {
        NotificationResponse r = notificationService.getNotificationDetailByEmail(jwt.getSubject(), id);
        return r == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(r);
    }

    @PatchMapping("/me/{id}/read")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") String id
    ) {
        notificationService.markAsReadByEmail(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/read-all")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsReadByEmail(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") String id
    ) {
        notificationService.deleteNotificationByEmail(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }
}
