package com.privsense.api.service.auth;

import com.privsense.api.dto.auth.LoginRequest;
import com.privsense.api.dto.auth.TokenResponse;
import org.springframework.security.core.Authentication;

/**
 * Service interface for authentication operations.
 * Provides methods for login, token refreshing, and logout operations.
 */
public interface AuthenticationService {

    /**
     * Authenticate a user and generate tokens.
     * 
     * @param request Login credentials
     * @return Response containing access and refresh tokens
     */
    TokenResponse authenticate(LoginRequest request);
    
    /**
     * Generate a new access token using a refresh token.
     * 
     * @param refreshToken The refresh token to use
     * @return Response containing new access token and the same refresh token
     */
    TokenResponse refreshToken(String refreshToken);
    
    /**
     * Invalidate a token when a user logs out.
     * 
     * @param token The token to invalidate
     */
    void logout(String token);
    
    /**
     * Validate a JWT token and create an Authentication object.
     * 
     * @param token JWT token to validate
     * @return Authentication object if token is valid, null otherwise
     */
    Authentication validateToken(String token);
}