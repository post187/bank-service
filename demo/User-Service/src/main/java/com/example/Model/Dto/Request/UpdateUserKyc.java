package com.example.Model.Dto.Request;

import com.example.Model.Dto.Internal.StatusUserService.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserKyc {
    private String kycId;
    private String submittedIdentificationNumber;
    private String submittedFullName;
    private DocumentType documentType;
    private String frontCardUrl;
    private String backCardUrl;
    private String selfieUrl;
}
