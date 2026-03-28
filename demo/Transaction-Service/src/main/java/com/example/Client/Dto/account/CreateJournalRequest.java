package com.example.Client.Dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJournalRequest {
    private ReferenceType referenceType;
    private Long referenceId;
    private String description;
    private String createdBy;
    private List<LedgerLineRequest> entries;
}
