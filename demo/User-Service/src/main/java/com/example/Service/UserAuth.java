package com.example.Service;

import com.example.Model.Dto.Internal.*;
import com.example.Model.Dto.Request.ChangePasswordRequest;
import com.example.Model.Dto.Request.LoginRequest;
import com.example.Model.Dto.Request.ResetPasswordRequest;
import com.example.Model.Dto.Request.VerifyDeviceRequest;
import com.example.Model.Dto.Response.*;

public interface UserAuth {

    //auth
    Response createUser(UserCreate userDto);

    JwtResponse login(LoginRequest request);

    JwtResponse verifyDeviceAndLogin(VerifyDeviceRequest request);

    Response logout();

    JwtResponse refreshToken(String refreshToken);

    Response changePassword(ChangePasswordRequest request);

    Response resetPassword(ResetPasswordRequest request);

    Response forgotPassword(String email);

    UserDto readUserByAccountId(Long accountId);

    Response verifyToken(String tokenValue);

    Response sendCode(String email);
}
