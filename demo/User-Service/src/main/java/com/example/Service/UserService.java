package com.example.Service;

import com.example.Model.Dto.Internal.*;
import com.example.Model.Dto.Internal.Status.KycStatus;
import com.example.Model.Dto.Request.ChangePasswordRequest;
import com.example.Model.Dto.Request.LoginRequest;
import com.example.Model.Dto.Request.ResetPasswordRequest;
import com.example.Model.Dto.Request.VerifyDeviceRequest;
import com.example.Model.Dto.Response.*;

import java.util.List;

public interface UserService {

    //auth
    Response createUser(CreateUser userDto);

    JwtResponse login(LoginRequest request);

    JwtResponse verifyDeviceAndLogin(VerifyDeviceRequest request);

    Response logout(String sessionId);

    JwtResponse refreshToken(String sessionId, String refreshToken);

    Response changePassword(ChangePasswordRequest request, String sessionId);

    Response resetPassword(ResetPasswordRequest request);

    Response forgotPassword(String email);

    //User
    List<UserDto> readAllUsers(int page);

    UserDto getMyInfo();

    UserDto changeContactNumber(String contactNumber);

    UserDto changeUserProfile( UpdateUserProfile profile);

    //Admin
    Response updateUserStatus(Long id, UpdateStatus userUpdate);

    Response updateUserProfile(Long id, UpdateUserProfile userUpdate);

    UserDto readUserById(Long userId);

    Response disableUser(Long userId);

    Response enableUser(Long userId);

    Response addAdminRole(Long userId);

    //Kyc
    UserKycDto submitKyc(UpdateUserKyc request);

    List<UserKycDto> getMyKycHistory();

    UserKycDto getLatestKyc();

    Response updateKycStatus(Long kycId, KycStatus status, String reason);


//    UserDto readUserByAccountId(String accountId);

    Response verifyToken(String tokenValue);

    Response sendCode(String email);
}
