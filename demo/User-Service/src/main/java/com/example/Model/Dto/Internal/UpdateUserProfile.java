package com.example.Model.Dto.Internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserProfile {
    private String firstName;

    private String lastName;

    private String contactNo;

    private String address;

    private String gender;

    private String occupation;

    private String martialStatus;

    private String nationality;

    private LocalDateTime dateOfBirth;
}
