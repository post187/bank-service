package com.example.Model.Dto.Internal.Status;

public enum KycStatus {
    NOT_SUBMITTED,   // Chưa gửi thông tin KYC
    PENDING,         // Đang chờ xác minh
    VERIFIED,        // Đã xác minh thành công
    REJECTED,        // Bị từ chối
    EXPIRED          // Giấy tờ hết hạn
}
