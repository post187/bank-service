package com.example.Service.Implementation;

import com.example.Model.Dto.Request.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, Object> kafka;

    private static final int OTP_EXPIRATION_MINUTES = 5;

    private String otpKey(Long userId, String deviceId) {
        return "device_otp:" + userId + ":" + deviceId;
    }

    private String resendCooldownKey(Long userId, String deviceId) {
        return "device_resend_cooldown:" + userId + ":" + deviceId;
    }

    private String resendCountKey(Long userId, String deviceId) {
        return "device_resend_count:" + userId + ":" + deviceId;
    }

    private void generateAndSendOtp(Long userId, LoginRequest request) {

        int otpValue = 100000 + new SecureRandom().nextInt(900000);
        String otp = String.valueOf(otpValue);

        redis.opsForValue().set(
                otpKey(userId, request.getDeviceId()),
                otp,
                Duration.ofMinutes(OTP_EXPIRATION_MINUTES)
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("email", request.getEmail());
        payload.put("otp", otp);
        payload.put("deviceName", request.getDeviceName());
        payload.put("ip", request.getIpAddress());

        kafka.send("verify-new-device", payload);
    }

    public void sendOtpForNewDevice(Long userId, LoginRequest request) {
        generateAndSendOtp(userId, request);
    }

    public void resendOtpForNewDevice(Long userId, LoginRequest request) {

        String deviceId = request.getDeviceId();

        // cooldown 30s
        if (Boolean.TRUE.equals(redis.hasKey(resendCooldownKey(userId, deviceId)))) {
            throw new RuntimeException("Wait 30s before resend");
        }

        // limit 5 lần / 10 phút
        Long count = redis.opsForValue().increment(resendCountKey(userId, deviceId));

        if (count == 1) {
            redis.expire(resendCountKey(userId, deviceId), Duration.ofMinutes(10));
        }

        if (count > 5) {
            throw new RuntimeException("Too many resend attempts");
        }

        generateAndSendOtp(userId, request);

        redis.opsForValue().set(
                resendCooldownKey(userId, deviceId),
                "1",
                Duration.ofSeconds(30)
        );
    }
}
