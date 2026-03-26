package com.example.Model.Entity;

import com.example.Model.Status.JournalStatus;
import com.example.Model.Status.ReferenceType;
import jakarta.persistence.*;
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
@Entity
@Table(name = "ledger_journals")
public class LedgerJournal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long journalId;

    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;

    private Long referenceId;
    private String description;

    @Enumerated(EnumType.STRING)
    private JournalStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime postedAt;

    @OneToMany(mappedBy = "journal", cascade = CascadeType.ALL)
    private List<LedgerEntry> entries;
}
