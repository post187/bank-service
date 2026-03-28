package com.example.Exception;

import com.example.Model.Entity.MonetaryTransaction;

public class ConcurrentIdempotencyException extends RuntimeException {
    public MonetaryTransaction existing;

    public ConcurrentIdempotencyException(MonetaryTransaction existing) {
        this.existing = existing;
    }

    public MonetaryTransaction getExisting() {
        return existing;
    }
}
