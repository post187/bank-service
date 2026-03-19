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

    UserDto getMyInfo(String email);

    UserDto changeContactNumber(String email, String contactNumber);

    UserDto changeUserProfile(String email, UpdateUserProfile profile);

    Response updateUserStatus(Long id, UpdateStatus userUpdate);

    Response updateUserProfile(Long id, UpdateUserProfile userUpdate);

    UserDto readUserById(Long userId);

//    UserDto readUserByAccountId(String accountId);

    Response addAdminRole(Long userId);

    Response verifyToken(String tokenValue);

    Response sendCode(String email);
}
