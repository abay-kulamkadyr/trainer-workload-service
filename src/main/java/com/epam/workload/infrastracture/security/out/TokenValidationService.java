package com.epam.workload.infrastracture.security.out;

import com.epam.workload.infrastracture.security.core.TokenData;

public interface TokenValidationService {

    // if parsable return TokenData, if not throw
    TokenData parseToken(String token) throws IllegalArgumentException;

    // if valid -> TokenData, if not throw
    TokenData validateToken(String token) throws IllegalArgumentException;

}
