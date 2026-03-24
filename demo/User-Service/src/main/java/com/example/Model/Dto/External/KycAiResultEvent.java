package com.example.Model.Dto.External;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycAiResultEvent {
    private Long kycId;
    private Long userId;
    private String ocrIdNumber;
    private String ocrFullName;
    private Double faceMatchScore;
    private Boolean potentiallyFake;
}