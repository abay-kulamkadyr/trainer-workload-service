package com.epam.workload.infrastructure.security.adapter;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import javax.crypto.SecretKey;

import com.epam.workload.infrastructure.security.core.TokenData;
import com.epam.workload.infrastructure.security.out.TokenValidationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenValidationAdapter implements TokenValidationService {

    private final SecretKey signingKey;

    private final Clock clock;

    @Autowired
    JwtTokenValidationAdapter(@Value("${security.jwt.secret}") String secret, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    @Override
    public TokenData parseToken(String token) throws IllegalArgumentException {
        try {
            Claims claims = parseClaims(token);
            return new TokenData(claims.getSubject(),
                    claims.getIssuedAt().toInstant(),
                    claims.getExpiration().toInstant());
        }
        catch (JwtException | IllegalArgumentException e) {
            log.error("Failed to parse token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    @Override
    public TokenData validateToken(String token) throws IllegalArgumentException {
        try {
            TokenData tokenData = parseToken(token);
            boolean expired = tokenData.isExpired(clock.instant());

            if (expired) {
                log.debug("Token validation failed: token expired");
                throw new IllegalArgumentException("Token validation failed: token expired");
            }

            log.trace("Token validation successful");
            return tokenData;
        }
        catch (IllegalArgumentException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            throw e;
        }
    }

    private Claims parseClaims(String token) throws JwtException, IllegalArgumentException {
        return Jwts
                .parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
