package com.example.Model.Dto.Request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyDeviceRequest {
    private Long userId;
    private String deviceId;
    private String otp;

    private String deviceName;
    private String ipAddress;

}
