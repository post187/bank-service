package com.example.Service;

import com.example.Model.Dto.Internal.*;
import com.example.Model.Dto.Response.CreateResponse;
import com.example.Model.Dto.Response.JwtResponse;
import com.example.Model.Dto.Response.Response;

import java.util.List;

public interface UserService {

    CreateResponse createUser(CreateUser userDto);

    JwtResponse login(UserLogin login);

    List<UserDto> readAllUsers(int page);

    UserDto getMyInfo();

    UserDto changeContactNumber(String contactNumber);

    UserDto changeUserProfile( UpdateUserProfile profile);

    Response updateUserStatus(Long id, UpdateStatus userUpdate);

    Response updateUserProfile(Long id, UpdateUserProfile userUpdate);

    UserDto readUserById(Long userId);

//    UserDto readUserByAccountId(String accountId);

    Response addAdminRole(Long userId);

    Response verifyToken(String tokenValue);

    Response sendCode(String email);
}
