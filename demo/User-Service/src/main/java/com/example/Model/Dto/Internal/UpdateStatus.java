package com.example.Model.Dto.Internal;

import com.example.Model.Dto.Internal.StatusUserService.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStatus {
    private Status status;
}
