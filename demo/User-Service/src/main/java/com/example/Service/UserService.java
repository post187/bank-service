package com.example.Service;

import com.example.Model.Dto.Internal.CreateUser;
import com.example.Model.Dto.Internal.UpdateStatus;
import com.example.Model.Dto.Internal.UpdateUserProfile;
import com.example.Model.Dto.Internal.UserDto;
import com.example.Model.Dto.Response.Response;

import java.util.List;

public interface UserService {

    Response createUser(CreateUser userDto);

    List<UserDto> readAllUsers();

    UserDto readUser(String authId);

    Response updateUserStatus(Long id, UpdateStatus userUpdate);

    Response updateUser(Long id, UpdateUserProfile userUpdate);

    UserDto readUserById(Long userId);

    UserDto readUserByAccountId(String accountId);

    void addAdminRole(Long userId);
}
