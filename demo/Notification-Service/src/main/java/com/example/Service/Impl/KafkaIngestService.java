package com.example.Service.Impl;

import com.example.Client.AccountInternalClient;
import com.example.Client.Dto.AccountOwnerResponse;
import com.example.Client.Dto.UserEmailResponse;
import com.example.Client.UserInternalClient;
import com.example.Constant.KafkaTopics;
import com.example.Model.Document.Email;
import com.example.Model.Document.Notification;
import com.example.Model.Dto.EmailDetail;
import com.example.Repository.EmailRepository;
import com.example.Repository.NotificationRepository;
import com.example.Service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaIngestService {

    private final NotificationRepository notificationRepository;
    private final EmailRepository emailRepository;
    private final EmailService emailService;
    private final UserInternalClient userInternalClient;
    private final AccountInternalClient accountInternalClient;
    private final ObjectMapper objectMapper;

    public void onRegistration(String email, String token) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token", token);
        persistAndMaybeEmail(
                null,
                email,
                "Xác thực đăng ký",
                "Mã xác thực tài khoản của bạn: " + token,
                "REGISTRATION",
                "USER_SERVICE",
                KafkaTopics.REGISTRATION,
                email,
                payload,
                true,
                "Verify your account"
        );
    }

    public void onResetPassword(String email, String secretPayload) {
        boolean looksLikeOtp = secretPayload != null && secretPayload.matches("\\d{6}");
        String title = looksLikeOtp ? "Mã OTP đặt lại mật khẩu" : "Mật khẩu mới của bạn";
        String body = looksLikeOtp
                ? "Mã OTP đặt lại mật khẩu: " + secretPayload + " (có hiệu lực trong thời gian quy định)."
                : "Mật khẩu mới sau khi đặt lại: " + secretPayload + ". Vui lòng đăng nhập và đổi mật khẩu ngay.";
        Map<String, Object> payload = Map.of("type", looksLikeOtp ? "OTP" : "NEW_PASSWORD");
        persistAndMaybeEmail(
                null,
                email,
                title,
                body,
                "SECURITY",
                "USER_SERVICE",
                KafkaTopics.RESET_PASSWORD,
                email,
                payload,
                true,
                looksLikeOtp ? "Password reset OTP" : "Your new password"
        );
    }

    public void onKycUserMessage(String email, String message) {
        Map<String, Object> payload = Map.of("raw", message);
        persistAndMaybeEmail(
                null,
                email,
                "Thông báo KYC",
                message,
                "KYC",
                "USER_SERVICE",
                KafkaTopics.KYC_USER,
                email,
                payload,
                true,
                "KYC update"
        );
    }

    public void onAbleUser(String email, String message) {
        Map<String, Object> payload = Map.of("raw", message);
        persistAndMaybeEmail(
                null,
                email,
                "Trạng thái tài khoản",
                message,
                "ACCOUNT",
                "USER_SERVICE",
                KafkaTopics.ABLE_USER,
                email,
                payload,
                true,
                "Account status"
        );
    }

    public void onVerifyNewDevice(String jsonPayload) {
        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            Long userId = root.path("userId").asLong();
            String email = root.path("email").asText(null);
            String otp = root.path("otp").asText();
            String deviceName = root.path("deviceName").asText("");
            String body = String.format(
                    "Xác thực thiết bị mới. OTP: %s. Thiết bị: %s. Nếu không phải bạn, hãy liên hệ ngân hàng ngay.",
                    otp, deviceName
            );
            Map<String, Object> payload = objectMapper.convertValue(root, Map.class);
            persistAndMaybeEmail(
                    userId,
                    email,
                    "Xác thực thiết bị mới",
                    body,
                    "SECURITY",
                    "USER_SERVICE",
                    KafkaTopics.VERIFY_NEW_DEVICE,
                    String.valueOf(userId),
                    payload,
                    true,
                    "New device verification"
            );
        } catch (Exception e) {
            log.error("verify-new-device parse error: {}", e.getMessage());
        }
    }

    public void onAccountStringEvent(String topic, String userIdKey, String message) {
        Long uid = parseLong(userIdKey);
        Map<String, Object> payload = Map.of("message", message);
        String title = switch (topic) {
            case KafkaTopics.ACCOUNT_CREATED -> "Tài khoản đã được tạo";
            case KafkaTopics.ACCOUNT_STATUS_CHANGED -> "Cập nhật trạng thái tài khoản";
            case KafkaTopics.ACCOUNT_CLOSED -> "Tài khoản đã đóng";
            default -> "Thông báo tài khoản";
        };
        persistAndMaybeEmail(
                uid,
                null,
                title,
                message,
                "ACCOUNT",
                "ACCOUNT_SERVICE",
                topic,
                userIdKey,
                payload,
                true,
                title
        );
    }

    public void onLedgerJournalPosted(String key, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode entries = root.path("entries");
            if (!entries.isArray()) {
                return;
            }
            Set<Long> userIds = new HashSet<>();
            for (JsonNode e : entries) {
                long accountId = e.path("accountId").asLong();
                if (accountId <= 0) {
                    continue;
                }
                try {
                    AccountOwnerResponse owner = accountInternalClient.getOwner(accountId);
                    if (owner.getUserId() != null) {
                        userIds.add(owner.getUserId());
                    }
                } catch (Exception ex) {
                    log.warn("ledger owner lookup account {}: {}", accountId, ex.getMessage());
                }
            }
            String desc = root.path("description").asText("");
            String refType = root.path("referenceType").asText("");
            Long journalId = root.path("journalId").asLong();
            Map<String, Object> payload = new HashMap<>();
            payload.put("journalId", journalId);
            payload.put("referenceType", refType);
            payload.put("raw", json);

            for (Long uid : userIds) {
                String email = resolveEmail(uid);
                String body = String.format(
                        "Bút toán sổ cái #%s (loại %s) đã ghi nhận. %s",
                        journalId,
                        refType,
                        StringUtils.hasText(desc) ? desc : ""
                );
                persistAndMaybeEmail(
                        uid,
                        email,
                        "Giao dịch sổ cái",
                        body,
                        "LEDGER",
                        "ACCOUNT_SERVICE",
                        KafkaTopics.LEDGER_JOURNAL_POSTED,
                        key,
                        payload,
                        true,
                        "Ledger journal posted"
                );
            }
        } catch (Exception e) {
            log.error("ledger-journal-posted: {}", e.getMessage());
        }
    }

    public void onHoldCreated(String key, String json) {
        ingestAccountJsonEvent(key, json, KafkaTopics.HOLD_CREATED, "Ghi có giữ (hold)", "HOLD", "Hold created");
    }

    public void onHoldReleased(String key, String json) {
        ingestAccountJsonEvent(key, json, KafkaTopics.HOLD_RELEASED, "Giải tỏa hold", "HOLD", "Hold released");
    }

    public void onSnapshotCreated(String key, String json) {
        ingestAccountJsonEvent(key, json, KafkaTopics.ACCOUNT_SNAPSHOT_CREATED, "Snapshot số dư ngày", "SNAPSHOT", "Daily snapshot");
    }

    private void ingestAccountJsonEvent(String key, String json, String topic, String title, String category, String emailSubject) {
        try {
            JsonNode root = objectMapper.readTree(json);
            Long userId = root.path("userId").asLong();
            if (userId <= 0) {
                return;
            }
            String email = resolveEmail(userId);
            String body = root.toString();
            Map<String, Object> payload = objectMapper.convertValue(root, Map.class);
            persistAndMaybeEmail(
                    userId,
                    email,
                    title,
                    body,
                    category,
                    "ACCOUNT_SERVICE",
                    topic,
                    key,
                    payload,
                    true,
                    emailSubject
            );
        } catch (Exception e) {
            log.error("{}: {}", topic, e.getMessage());
        }
    }

    public void onTransactionCompleted(String key, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            Long initiatorUserId = root.path("initiatorUserId").asLong();
            String kind = root.path("kind").asText("");
            String amount = root.path("amount").asText("");
            String currency = root.path("currency").asText("");
            String body = String.format(
                    "Giao dịch %s hoàn tất. Số tiền: %s %s.",
                    kind,
                    amount,
                    currency
            );
            Map<String, Object> payload = objectMapper.convertValue(root, Map.class);
            String email = resolveEmail(initiatorUserId);
            persistAndMaybeEmail(
                    initiatorUserId,
                    email,
                    "Giao dịch hoàn tất",
                    body,
                    "TRANSACTION",
                    "TRANSACTION_SERVICE",
                    KafkaTopics.TRANSACTION_COMPLETED,
                    key,
                    payload,
                    true,
                    "Transaction completed"
            );
        } catch (Exception e) {
            log.error("transaction-completed: {}", e.getMessage());
        }
    }

    private void persistAndMaybeEmail(
            Long userId,
            String emailDirect,
            String title,
            String body,
            String category,
            String sourceService,
            String topic,
            String kafkaKey,
            Map<String, Object> payload,
            boolean trySendEmail,
            String emailSubject
    ) {
        String email = StringUtils.hasText(emailDirect) ? emailDirect.trim() : resolveEmail(userId);

        Notification n = Notification.builder()
                .userId(userId)
                .recipientEmail(email)
                .title(title)
                .body(body)
                .category(category)
                .sourceService(sourceService)
                .sourceTopic(topic)
                .kafkaMessageKey(kafkaKey)
                .payload(payload)
                .emailDeliveryStatus(email == null ? "SKIPPED" : "PENDING")
                .read(false)
                .build();
        n = notificationRepository.save(n);

        if (!trySendEmail || !StringUtils.hasText(email)) {
            n.setEmailDispatched(false);
            n.setEmailDeliveryStatus("SKIPPED");
            notificationRepository.save(n);
            return;
        }

        Email emailLog = Email.builder()
                .notificationId(n.getId())
                .userId(userId)
                .sourceTopic(topic)
                .sourceService(sourceService)
                .recipient(email)
                .subject(StringUtils.hasText(emailSubject) ? emailSubject : title)
                .body(body)
                .status("SENDING")
                .build();
        emailLog = emailRepository.save(emailLog);

        try {
            EmailDetail detail = new EmailDetail();
            detail.setRecipient(email);
            detail.setSubject(emailLog.getSubject());
            detail.setMsBody(body);
            emailService.sendSimpleMail(detail);
            emailLog.setStatus("SUCCESS");
            emailLog.setSentAt(LocalDateTime.now());
            emailRepository.save(emailLog);
            n.setEmailDispatched(true);
            n.setEmailDeliveryStatus("SENT");
            n.setRelatedEmailLogId(emailLog.getId());
            notificationRepository.save(n);
        } catch (Exception e) {
            log.warn("Email send failed {}: {}", email, e.getMessage());
            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage(e.getMessage());
            emailRepository.save(emailLog);
            n.setEmailDispatched(false);
            n.setEmailDeliveryStatus("FAILED");
            notificationRepository.save(n);
        }
    }

    private String resolveEmail(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            UserEmailResponse r = userInternalClient.getEmail(userId);
            return r != null ? r.getEmail() : null;
        } catch (Exception e) {
            log.debug("resolveEmail {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private static Long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }
}
