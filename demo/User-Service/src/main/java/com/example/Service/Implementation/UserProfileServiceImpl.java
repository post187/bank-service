package com.example.Service.Implementation;

import com.example.Exception.ResourceNotFoundException;
import com.example.Jwt.CustomerAuthentication.CustomAuthentication;
import com.example.Jwt.UserDetail.UserPrinciple;
import com.example.Model.Dto.Internal.UpdateUserProfile;
import com.example.Model.Dto.Response.UserDto;
import com.example.Model.Entity.User;
import com.example.Model.Entity.UserProfile;
import com.example.Model.Mapper.UserMapper;
import com.example.Repository.UserRepository;
import com.example.Service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final UserRepository userRepository;
    private UserMapper userMapper = new UserMapper();

    private String getMyEmail() {
        Authentication customAuthentication = (CustomAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if (customAuthentication == null) return null;
        Object principal = customAuthentication.getPrincipal();

        if (principal instanceof CustomAuthentication customAuth) {
            return customAuth.getEmail();
        } else if (principal instanceof UserPrinciple userPrinciple) {
            return userPrinciple.email();
        }
        return null;
    }


    @Override
    public UserDto getMyInfo() {
        String email = getMyEmail();
        return userRepository.findByEmail(email)
                .map(user -> userMapper.convertToDto(user))
                .orElseThrow(() -> new ResourceNotFoundException("User not found on the server"));
    }

    @Override
    public UserDto changeContactNumber(String contactNumber) {
        String email = getMyEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on the servers"));

        user.setContactNo(contactNumber);

        userRepository.save(user);

        return userMapper.convertToDto(user);
    }

    @Override
    public UserDto changeUserProfile(UpdateUserProfile profile) {
        String email = getMyEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on the servers"));

        UserProfile userProfile = user.getUserProfile();
        if (userProfile == null) {
            userProfile = new UserProfile();
            user.setUserProfile(userProfile);
        }

        BeanUtils.copyProperties(profile, userProfile);
        userRepository.save(user);

        return userMapper.convertToDto(user);
    }

}
