package com.example.Model.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.pl.NIP;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    private String responseCode;

    private String responseMessage;
}
