package com.example.Service;

import com.example.Model.Dto.Request.LoginRequest;
import com.example.Model.Dto.Request.VerifyDeviceRequest;
import com.example.Model.Entity.User;

public interface DeviceService {
    boolean isNewDevice(Long userId, String deviceId);
    void saveDevice(User user, LoginRequest request);
    void handleNewDevice(Long userId, LoginRequest request);
    void verifyDeviceOtp(VerifyDeviceRequest request);
}
