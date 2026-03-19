package com.example.Model.Dto.Internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUser {
    private String firstName;

    private String lastName;

    private String contactNumber;

    private String identificationNumber;

    private String email;

    private String password;

}
