package com.example.Service;

import com.example.Model.Dto.Request.CreateJournalRequest;
import com.example.Model.Dto.Response.LedgerEntryResponse;
import com.example.Model.Dto.Response.LedgerJournalResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface LedgerService {
    LedgerJournalResponse postJournal(CreateJournalRequest request);

    List<LedgerEntryResponse> getLedgerEntries(Long accountId, LocalDateTime from, LocalDateTime to);

    LedgerJournalResponse getJournalById(Long journalId);

    List<LedgerJournalResponse> getJournals(Long accountId);
}
