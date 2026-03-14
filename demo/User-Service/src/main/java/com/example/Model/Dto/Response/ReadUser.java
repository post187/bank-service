package com.example.Model.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadUser {
    private String firstName;
    private String lastName;

    private String email;

    private String contactNumber;

    private Status status;
}
