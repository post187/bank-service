package com.example.Model.Dto.External;

import com.example.Model.Status.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LedgerJournalPostedEvent {
    private Long journalId;
    private ReferenceType referenceType;
    private Long referenceId;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime postedAt;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private List<LedgerJournalPostedEntryEvent> entries;
}
