package com.example.Model.Dto.Response;

import com.example.Model.Status.JournalStatus;
import com.example.Model.Status.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LedgerJournalResponse {
    private Long journalId;
    private ReferenceType referenceType;
    private Long referenceId;
    private String description;
    private JournalStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime postedAt;
    private List<LedgerEntryResponse> entries;
}
