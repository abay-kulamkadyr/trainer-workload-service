package com.epam.workload.infrastructure.security.out;

import com.epam.workload.infrastructure.security.core.TokenData;

public interface TokenValidationService {

    // if parsable return TokenData, if not throw
    TokenData parseToken(String token) throws IllegalArgumentException;

    // if valid -> TokenData, if not throw
    TokenData validateToken(String token) throws IllegalArgumentException;
}
