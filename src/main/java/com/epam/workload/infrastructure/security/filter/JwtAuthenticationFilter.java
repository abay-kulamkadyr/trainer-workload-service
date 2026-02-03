package com.epam.workload.infrastructure.security.filter;

import java.io.IOException;

import com.epam.workload.infrastructure.security.authentication_token.JwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private static final int BEARER_PREFIX_LENGTH = 7;

    private final AuthenticationManager authenticationManager;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 1. Decide if this filter needs to handle authentication
        String requestUri = request.getRequestURI();
        String token = extractToken(request);

        if (token == null) {
            log.trace("No JWT token found in request to: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Authenticate
        try {
            // Delegate Authentication to Spring security
            Authentication authRequest = new JwtAuthenticationToken(token);
            Authentication authResult = authenticationManager.authenticate(authRequest);

            // Set authenticated context
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authResult);
            SecurityContextHolder.setContext(context);

            log.debug("Successfully authenticated request to: {} for user: {}", requestUri, authResult.getName());
        }
        catch (AuthenticationException e) {
            log.debug("Authentication failed for {}: {}", requestUri, e.getMessage());
            SecurityContextHolder.clearContext();
            throw e; // Let the exception propagate to be handled in
            // AuthenticationEntryPoint
        }

        // 3. Continue with the filter chain
        filterChain.doFilter(request, response);

        // No cleanup
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX_LENGTH);
    }

}
