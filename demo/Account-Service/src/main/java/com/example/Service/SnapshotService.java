package com.example.Service;

import com.example.Model.Dto.Response.SnapshotResponse;

import java.time.LocalDate;
import java.util.List;

public interface SnapshotService {
    SnapshotResponse createDailySnapshot(Long accountId, LocalDate date);

    List<SnapshotResponse> getDailySnapshots(Long accountId, LocalDate from, LocalDate to);
}
