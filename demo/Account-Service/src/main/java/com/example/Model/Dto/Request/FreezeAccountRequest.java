package com.example.Model.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FreezeAccountRequest {
    private Long accountId;

    private String reason;

    private String requestedBy;
}
