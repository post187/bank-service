package com.example.Model.Dto.External;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycAiCheckEvent {
    private Long kycId;
    private Long userId;
    private String frontCardUrl;
    private String backCardUrl;
    private String selfieUrl;
    private String submittedIdentificationNumber;
    private String submittedFullName;
}