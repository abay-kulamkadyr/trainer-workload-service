package com.epam.workload.infrastructure.security.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import javax.crypto.SecretKey;

import com.epam.workload.infrastructure.security.core.TokenData;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenValidationAdapterTest {

    private static final String SECRET = "my-super-secret-key-my-super-secret-key";

    private Clock clock;
    private JwtTokenValidationAdapter adapter;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2024-01-01T10:00:00Z"), ZoneOffset.UTC);
        adapter = new JwtTokenValidationAdapter(SECRET, clock);
    }

    private String generateToken(Instant issued, Instant expiry) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject("user1")
                .issuedAt(Date.from(issued))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    @Test
    void parseToken_validToken_returnsTokenData() {
        Instant issued = clock.instant();
        Instant expiry = issued.plusSeconds(3600);

        String token = generateToken(issued, expiry);

        TokenData data = adapter.parseToken(token);

        assertEquals("user1", data.username());
        assertEquals(issued, data.issuedAt());
        assertEquals(expiry, data.expiresAt());
    }

    @Test
    void parseToken_invalidToken_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> adapter.parseToken("invalid-token"));
    }

    @Test
    void validateToken_validToken_returnsTokenData() {
        Instant issued = clock.instant();
        Instant expiry = issued.plusSeconds(3600);

        String token = generateToken(issued, expiry);

        TokenData data = adapter.validateToken(token);

        assertEquals("user1", data.username());
    }

    @Test
    void validateToken_expiredToken_throwsException() {
        Instant issued = clock.instant().minusSeconds(7200);
        Instant expiry = clock.instant().minusSeconds(3600);

        String token = generateToken(issued, expiry);

        assertThrows(IllegalArgumentException.class, () -> adapter.validateToken(token));
    }
}
