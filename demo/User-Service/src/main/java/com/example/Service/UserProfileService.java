package com.example.Service;

import com.example.Model.Dto.Internal.UpdateUserProfile;
import com.example.Model.Dto.Response.UserDto;

public interface UserProfileService {

    UserDto getMyInfo();

    UserDto changeContactNumber(String contactNumber);

    UserDto changeUserProfile( UpdateUserProfile profile);
}
