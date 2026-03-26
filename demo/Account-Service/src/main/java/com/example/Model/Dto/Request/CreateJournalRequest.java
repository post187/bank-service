package com.example.Model.Dto.Request;

import com.example.Model.Status.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateJournalRequest {
    private ReferenceType referenceType;
    private Long referenceId;
    private String description;
    private String createdBy;
    private List<LedgerLineRequest> entries;
}
