package com.example.Model.Dto.Response;

import com.example.Model.Dto.Internal.Status.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    private Long userId;

    private String email;

    private String contactNo;

    private String identificationNumber;

    private boolean verifyEmail;

    private boolean enable;

    private Status status;

    private Set<String> roles;

    private LocalDate creationOn;

    private LocalDateTime lastLoginAt;

    private String kycStatus; // hoặc enum nếu bạn có

    private UserProfileDto userProfile;
}
