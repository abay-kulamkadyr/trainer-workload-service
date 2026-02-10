package com.epam.workload.infrastructure.security.core;

import java.time.Instant;

public record TokenData(String username, Instant issuedAt, Instant expiresAt) {
    public boolean isExpired(Instant currentTime) {
        return expiresAt.isBefore(currentTime);
    }
}
