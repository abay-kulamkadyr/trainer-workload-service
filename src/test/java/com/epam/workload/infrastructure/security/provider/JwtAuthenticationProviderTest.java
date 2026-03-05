package com.epam.workload.infrastructure.security.provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import com.epam.workload.infrastructure.security.authentication_token.JwtAuthenticationToken;
import com.epam.workload.infrastructure.security.core.TokenData;
import com.epam.workload.infrastructure.security.out.TokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationProviderTest {

    @Mock
    private TokenValidationService tokenValidationService;

    private JwtAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtAuthenticationProvider(tokenValidationService);
    }

    @Test
    void authenticate_validToken_returnsAuthentication() {
        TokenData data = new TokenData("user1", Instant.now(), Instant.now().plusSeconds(3600));

        when(tokenValidationService.validateToken("token")).thenReturn(data);

        Authentication auth = provider.authenticate(new JwtAuthenticationToken("token"));

        assertEquals("user1", auth.getName());
        assertTrue(auth.isAuthenticated());
    }

    @Test
    void authenticate_invalidToken_throwsException() {
        when(tokenValidationService.validateToken("token")).thenThrow(new IllegalArgumentException());

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(new JwtAuthenticationToken("token")));
    }

    @Test
    void supports_returnsTrueForJwtAuthenticationToken() {
        assertTrue(provider.supports(JwtAuthenticationToken.class));
    }
}
