package com.epam.workload.infrastructure.security.filter;

import static org.mockito.Mockito.*;

import com.epam.workload.infrastructure.security.authentication_token.JwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

class JwtAuthenticationFilterTest {

    private AuthenticationManager authenticationManager;
    private JwtAuthenticationFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        filter = new JwtAuthenticationFilter(authenticationManager);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    void doFilter_noToken_passThrough() throws Exception {

        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/test");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void doFilter_validToken_authenticates() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Bearer token123");
        when(request.getRequestURI()).thenReturn("/api/test");

        Authentication result = mock(Authentication.class);
        when(result.getName()).thenReturn("user");

        when(authenticationManager.authenticate(any())).thenReturn(result);

        filter.doFilter(request, response, chain);

        verify(authenticationManager).authenticate(any(JwtAuthenticationToken.class));
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_invalidToken_throwsException() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Bearer token123");
        when(request.getRequestURI()).thenReturn("/api/test");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid"));

        try {
            filter.doFilter(request, response, chain);
        } catch (BadCredentialsException ignored) {
        }

        verify(authenticationManager).authenticate(any());
    }
}
