package com.example.Service.Implementation;

import com.example.Model.Dto.External.KycAiResultEvent;
import com.example.Model.Dto.Internal.UserCreate;
import com.example.Model.Dto.Internal.StatusUserService.KycStatus;
import com.example.Model.Dto.Internal.StatusUserService.Status;
import com.example.Model.Dto.Internal.UpdateStatus;
import com.example.Model.Dto.Internal.UpdateUserProfile;
import com.example.Model.Dto.Request.ChangePasswordRequest;
import com.example.Model.Dto.Request.LoginRequest;
import com.example.Model.Dto.Request.ResetPasswordRequest;
import com.example.Model.Dto.Request.UpdateUserKyc;
import com.example.Model.Dto.Request.VerifyDeviceRequest;
import com.example.Model.Dto.Response.AccountEligibilityResponse;
import com.example.Model.Dto.Response.DeviceDto;
import com.example.Model.Dto.Response.JwtResponse;
import com.example.Model.Dto.Response.Response;
import com.example.Model.Dto.Response.UserDto;
import com.example.Model.Dto.Response.UserKycDtoAdmin;
import com.example.Model.Dto.Response.UserKycDtoUser;
import com.example.Model.Entity.User;
import com.example.Repository.UserKycRepository;
import com.example.Repository.UserRepository;
import com.example.Service.KycService;
import com.example.Service.UserAdminService;
import com.example.Service.UserAuth;
import com.example.Service.UserProfileService;
import com.example.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserAuth userAuthService;
    private final UserAdminService userAdminService;
    private final UserProfileService userProfileService;
    private final KycService kycService;
    private final UserRepository userRepository;
    private final UserKycRepository userKycRepository;

    @Override
    public Response createUser(UserCreate userDto) {
        return userAuthService.createUser(userDto);
    }

    @Override
    public JwtResponse login(LoginRequest request) {
        return userAuthService.login(request);
    }

    @Override
    public JwtResponse verifyDeviceAndLogin(VerifyDeviceRequest request) {
        return userAuthService.verifyDeviceAndLogin(request);
    }

    @Override
    public Response logout() {
        return userAuthService.logout();
    }

    @Override
    public JwtResponse refreshToken(String refreshToken) {
        return userAuthService.refreshToken(refreshToken);
    }

    @Override
    public Response changePassword(ChangePasswordRequest request) {
        return userAuthService.changePassword(request);
    }

    @Override
    public Response resetPassword(ResetPasswordRequest request) {
        return userAuthService.resetPassword(request);
    }

    @Override
    public Response forgotPassword(String email) {
        return userAuthService.forgotPassword(email);
    }

    @Override
    public UserDto readUserByAccountId(Long accountId) {
        return userAuthService.readUserByAccountId(accountId);
    }

    @Override
    public Response verifyToken(String tokenValue) {
        return userAuthService.verifyToken(tokenValue);
    }

    @Override
    public Response sendCode(String email) {
        return userAuthService.sendCode(email);
    }

    @Override
    public List<UserDto> readAllUsers(int page) {
        return userAdminService.readAllUsers(page);
    }

    @Override
    public Response updateUserStatus(Long id, UpdateStatus userUpdate) {
        return userAdminService.updateUserStatus(id, userUpdate);
    }

    @Override
    public Response updateUserProfile(Long id, UpdateUserProfile userUpdate) {
        return userAdminService.updateUserProfile(id, userUpdate);
    }

    @Override
    public UserDto readUserById(Long userId) {
        return userAdminService.readUserById(userId);
    }

    @Override
    public Response disableUser(Long userId) {
        return userAdminService.disableUser(userId);
    }

    @Override
    public Response enableUser(Long userId) {
        return userAdminService.enableUser(userId);
    }

    @Override
    public Response addAdminRole(Long userId) {
        return userAdminService.addAdminRole(userId);
    }

    @Override
    public Response revokeDevice(Long userId, String deviceId) {
        return userAdminService.revokeDevice(userId, deviceId);
    }

    @Override
    public Response forceLogout(Long userId) {
        return userAdminService.forceLogout(userId);
    }

    @Override
    public List<DeviceDto> getUserDevices(Long userId) {
        return userAdminService.getUserDevices(userId);
    }

    @Override
    public Page<UserKycDtoAdmin> getPendingKyc(Pageable pageable) {
        return userAdminService.getPendingKyc(pageable);
    }

    @Override
    public Response approveKyc(Long kycId, String adminNote) {
        return userAdminService.approveKyc(kycId, adminNote);
    }

    @Override
    public Response rejectKyc(Long kycId, String rejectionReason, String adminNote) {
        return userAdminService.rejectKyc(kycId, rejectionReason, adminNote);
    }

    @Override
    public Page<UserKycDtoAdmin> getUpdateKyc(Pageable pageable) {
        return userAdminService.getUpdateKyc(pageable);
    }

    @Override
    public Response approveProfileChange(Long kycId, String adminNote) {
        return userAdminService.approveProfileChange(kycId, adminNote);
    }

    @Override
    public Response rejectProfileChange(Long kycId, String reason) {
        return userAdminService.rejectProfileChange(kycId, reason);
    }

    @Override
    public UserDto getMyInfo() {
        return userProfileService.getMyInfo();
    }

    @Override
    public UserDto changeContactNumber(String contactNumber) {
        return userProfileService.changeContactNumber(contactNumber);
    }

    @Override
    public UserDto changeUserProfile(UpdateUserProfile profile) {
        return userProfileService.changeUserProfile(profile);
    }

    @Override
    public UserKycDtoUser submitKyc(UpdateUserKyc request) {
        return kycService.submitKyc(request);
    }

    @Override
    public List<UserKycDtoUser> getMyKycHistory() {
        return kycService.getMyKycHistory();
    }

    @Override
    public UserKycDtoUser getLatestKyc() {
        return kycService.getLatestKyc();
    }

    @Override
    public void processAiResult(KycAiResultEvent resultEvent) {
        kycService.processAiResult(resultEvent);
    }

    @Override
    public Response updateKycStatus(Long kycId, KycStatus status, String reason) {
        return kycService.updateKycStatus(kycId, status, reason);
    }

    @Override
    public AccountEligibilityResponse getAccountEligibility(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return AccountEligibilityResponse.builder()
                    .userId(userId)
                    .userExists(false)
                    .active(false)
                    .kycVerified(false)
                    .eligible(false)
                    .latestKycStatus(KycStatus.NOT_SUBMITTED.name())
                    .reason("User not found")
                    .build();
        }

        boolean active = user.isEnable() && user.isVerifyEmail() && user.getStatus() == Status.APPROVED;
        KycStatus latestKycStatus = userKycRepository.findTopByUser_UserIdOrderBySubmittedAtDesc(userId)
                .map(kyc -> kyc.getStatus())
                .orElse(KycStatus.NOT_SUBMITTED);
        boolean kycVerified = latestKycStatus == KycStatus.VERIFIED;
        boolean eligible = active && kycVerified;

        String reason;
        if (!active) {
            reason = "User account is not active or not verified";
        } else if (!kycVerified) {
            reason = "User KYC is not verified";
        } else {
            reason = "User is eligible to create account";
        }

        return AccountEligibilityResponse.builder()
                .userId(userId)
                .userExists(true)
                .active(active)
                .kycVerified(kycVerified)
                .eligible(eligible)
                .latestKycStatus(latestKycStatus.name())
                .reason(reason)
                .build();
    }

    @Override
    public String getEmailByUserIdInternal(Long userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElse(null);
    }

    @Override
    public Long getUserIdByEmailInternal(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmail(email.trim())
                .map(User::getUserId)
                .orElse(null);
    }
}
