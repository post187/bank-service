package com.example.Model.Dto.Internal;

import com.example.Model.Dto.Internal.Status.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserKyc {
    @NotBlank
    private String identificationNumber;

    @NotNull
    private DocumentType documentType;

    @NotBlank
    private String selfieUrl;
}
