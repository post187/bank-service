package com.example.Model.Mapper;

import com.example.Model.Dto.Response.UserDto;
import com.example.Model.Dto.Response.UserProfileDto;
import com.example.Model.Entity.User;
import com.example.Model.Entity.UserProfile;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;

import java.util.Objects;

public class UserMapper extends BaseMapper<User, UserDto> {

    private ModelMapper mapper = new ModelMapper();
    @Override
    public User convertToEntity(UserDto dto, Object... args) {
        User user = new User();
        if (Objects.isNull(dto)) {
            BeanUtils.copyProperties(dto, user);
            if(!Objects.isNull(dto.getUserProfile())){
                UserProfile userProfile = new UserProfile();
                BeanUtils.copyProperties(dto.getUserProfile(), userProfile);
                user.setUserProfile(userProfile);
            }
        }
        return user;
    }

    @Override
    public UserDto convertToDto(User entity, Object... args) {
        UserDto userDto = new UserDto();
        if(!Objects.isNull(entity)){
            BeanUtils.copyProperties(entity, userDto);
            if(!Objects.isNull(entity.getUserProfile())) {
                UserProfileDto userProfileDto = new UserProfileDto();
                BeanUtils.copyProperties(entity.getUserProfile(), userProfileDto);
                userDto.setUserProfile(userProfileDto);
            }
        }
        return userDto;
    }
}
