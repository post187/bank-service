package com.example.Service;

import com.example.Model.Dto.Internal.UpdateStatus;
import com.example.Model.Dto.Internal.UpdateUserProfile;
import com.example.Model.Dto.Response.DeviceDto;
import com.example.Model.Dto.Response.Response;
import com.example.Model.Dto.Response.UserDto;
import com.example.Model.Dto.Response.UserKycDtoAdmin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserAdminService {
    //User
    List<UserDto> readAllUsers(int page);

    Response updateUserStatus(Long id, UpdateStatus userUpdate);

    Response updateUserProfile(Long id, UpdateUserProfile userUpdate);

    UserDto readUserById(Long userId);

    Response disableUser(Long userId);

    Response enableUser(Long userId);

    Response addAdminRole(Long userId);

    Response revokeDevice(Long userId, String deviceId);

    Response forceLogout(Long userId);

    List<DeviceDto> getUserDevices(Long userId);

    Page<UserKycDtoAdmin> getPendingKyc(Pageable pageable);

    Response approveKyc(Long kycId, String adminNote);
    Response rejectKyc(Long kycId, String rejectionReason, String adminNote);

    Page<UserKycDtoAdmin> getUpdateKyc(Pageable pageable);

    Response approveProfileChange(Long kycId, String adminNote);
    Response rejectProfileChange(Long kycId, String reason);
}
