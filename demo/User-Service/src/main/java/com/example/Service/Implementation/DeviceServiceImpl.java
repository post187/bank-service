package com.example.Service.Implementation;

import com.example.Exception.DeviceLockedException;
import com.example.Exception.DeviceVerificationRequiredException;
import com.example.Exception.OtpInvalidException;
import com.example.Model.Dto.Request.LoginRequest;
import com.example.Model.Dto.Request.VerifyDeviceRequest;
import com.example.Model.Entity.User;
import com.example.Model.Entity.UserDevice;
import com.example.Repository.UserDeviceRepository;
import com.example.Service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.security.CryptoPrimitive.SECURE_RANDOM;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {
    private final StringRedisTemplate redis;
    private final UserDeviceRepository userDeviceRepository;
    private final OtpService otpService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long OTP_EXPIRATION_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    @Override
    public boolean isNewDevice(Long userId, String deviceId) {
        // Trả về true nếu KHÔNG tìm thấy thiết bị này đang active trong DB
        return userDeviceRepository.findByUserIdAndDeviceIdAndIsActiveTrue(userId, deviceId).isEmpty();
    }

    @Override
    @Transactional
    public void saveDevice(User user, LoginRequest request) {
        UserDevice newDevice = UserDevice.builder()
                .userId(user.getUserId())
                .deviceId(request.getDeviceId())
                .deviceName(request.getDeviceName())
                .ipAddress(request.getIpAddress())
                .lastLoginAt(LocalDateTime.now())
                .build();

        userDeviceRepository.save(newDevice);
    }

    @Override
    public void handleNewDevice(Long userId, LoginRequest request) {
        String deviceId = request.getDeviceId();

        String lockKey = "device lock:" + userId + ":" + deviceId;

        if (Boolean.TRUE.equals(redis.hasKey(lockKey))) {
            throw new RuntimeException("Device is temporarily locked. Try later");
        }

        otpService.sendOtpForNewDevice(userId, request);

        // 👉 throw để FE biết cần verify
        throw new DeviceVerificationRequiredException(
                "New device. Please check email",
                userId
        );
    }


    @Override
    @Transactional
    public void verifyDeviceOtp(VerifyDeviceRequest request) {
        Long userId = request.getUserId();
        String deviceId = request.getDeviceId();
        String inputOtp = request.getOtp();

        String lockKey = "device_lock:" + userId + ":" + deviceId;
        if (Boolean.TRUE.equals(redis.hasKey(lockKey))) {
            // Tùy chọn: Lấy ra số phút còn lại để báo cho user
            Long expireTime = redis.getExpire(lockKey);
            long minutesLeft = expireTime != null ? expireTime / 60 : 30;
            throw new DeviceLockedException("Your device is locked. Please try again" + minutesLeft + " phút.");
        }

        String otpKey = "device_otp:" + userId + ":" + deviceId;
        String validOtp = (String) redis.opsForValue().get(otpKey);

        String attemptKey = "device_otp_attempts:" + userId + ":" + deviceId;

        if (validOtp == null) {
            throw new OtpInvalidException("Otp not found on the servers");
        }
        if (!validOtp.equals(inputOtp)) {
            Long attempts = redis.opsForValue().increment(attemptKey);

            if (attempts != null && attempts == 1) {
                redis.expire(attemptKey, Duration.ofMinutes(30));
            }
            long remaining = MAX_ATTEMPTS - (attempts != null ? attempts : 0);
            throw new OtpInvalidException("Mã OTP not correct. You have " + remaining + "attempts");
        }

        redis.delete(otpKey);
        redis.delete(attemptKey);

        UserDevice newDevice = UserDevice.builder()
                .userId(userId)
                .deviceId(deviceId)
                .deviceName(request.getDeviceName())
                .ipAddress(request.getIpAddress())
                .lastLoginAt(LocalDateTime.now())
                .build();

        userDeviceRepository.save(newDevice);

        log.info("Xác thực thiết bị thành công. Đã thêm máy {} vào danh sách tin cậy của User: {}", deviceId, userId);
    }
}
