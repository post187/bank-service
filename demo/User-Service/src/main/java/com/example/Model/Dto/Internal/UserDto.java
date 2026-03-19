package com.example.Model.Dto.Internal;

import com.example.Model.Entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long userId;

    private String email;

    private String identificationNumber;

    private Status status;

    private UserProfileDto userProfileDto;
}
