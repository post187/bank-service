package com.example.Exception;

public class DeviceVerificationRequiredException extends RuntimeException {
    private final Long userId;

    public DeviceVerificationRequiredException(String message, Long userId) {
        super(message);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    // Tắt sinh Stack Trace để tối ưu hiệu năng (như mình đã nói ở trên)
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}