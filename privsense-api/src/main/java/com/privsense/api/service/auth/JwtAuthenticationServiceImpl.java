package com.privsense.api.service.auth;

import com.privsense.api.dto.auth.LoginRequest;
import com.privsense.api.dto.auth.TokenResponse;
import com.privsense.api.exception.AuthenticationException;
import com.privsense.core.config.PrivSenseConfigProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * JWT-based implementation of the AuthenticationService.
 * Handles token generation, validation, and token blacklisting.
 */
@Slf4j
public class JwtAuthenticationServiceImpl implements AuthenticationService {

    private final Supplier<UserDetailsService> userDetailsServiceSupplier;
    private final PasswordEncoder passwordEncoder;
    private final PrivSenseConfigProperties configProperties;
    
    // In-memory token blacklist
    private final Map<String, Long> tokenBlacklist = new ConcurrentHashMap<>();
    
    // Scheduled executor for cleanup tasks
    private final ScheduledExecutorService scheduler;

    public JwtAuthenticationServiceImpl(
            Supplier<UserDetailsService> userDetailsServiceSupplier,
            PasswordEncoder passwordEncoder,
            PrivSenseConfigProperties configProperties) {
        this.userDetailsServiceSupplier = userDetailsServiceSupplier;
        this.passwordEncoder = passwordEncoder;
        this.configProperties = configProperties;
        
        // Create a cleanup scheduler that runs as a daemon thread
        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "token-blacklist-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        
        // Schedule periodic cleanup of expired blacklisted tokens
        scheduler.scheduleAtFixedRate(this::cleanupExpiredTokens, 
            10, 10, TimeUnit.MINUTES);
        
        log.info("JWT Authentication Service initialized with token blacklisting");
    }

    @Override
    public TokenResponse authenticate(LoginRequest request) {
        UserDetails userDetails = getUserDetailsService().loadUserByUsername(request.getUsername());
        
        if (userDetails == null || !passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            throw new AuthenticationException("Invalid username or password");
        }
        
        log.debug("User authenticated successfully: {}", request.getUsername());
        log.debug("User authorities: {}", userDetails.getAuthorities());
        
        return generateTokenResponse(userDetails);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        try {
            if (isTokenBlacklisted(refreshToken)) {
                throw new AuthenticationException("Refresh token has been invalidated");
            }
            
            Claims claims = extractAllClaims(refreshToken, getRefreshTokenKey());
            String username = claims.getSubject();
            
            UserDetails userDetails = getUserDetailsService().loadUserByUsername(username);
            if (userDetails == null) {
                throw new AuthenticationException("User not found");
            }
            
            // Invalidate the old refresh token to prevent reuse
            blacklistToken(refreshToken, getExpirationFromToken(refreshToken));
            
            // Generate new tokens
            return generateTokenResponse(userDetails);
        } catch (ExpiredJwtException e) {
            throw new AuthenticationException("Refresh token has expired");
        } catch (SignatureException e) {
            throw new AuthenticationException("Invalid refresh token");
        }
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            // Add to blacklist to prevent reuse
            blacklistToken(token, getExpirationFromToken(token));
            log.debug("Token added to blacklist");
        }
    }

    @Override
    public Authentication validateToken(String token) {
        if (token == null || token.isEmpty() || isTokenBlacklisted(token)) {
            return null;
        }
        
        try {
            Claims claims = extractAllClaims(token, getAccessTokenKey());
            String username = claims.getSubject();
            
            // Get roles from token
            String rolesString = claims.get("roles", String.class);
            Collection<? extends GrantedAuthority> authorities;
            
            // If there are no roles in the token, load the user from database to get fresh roles
            if (rolesString == null || rolesString.isEmpty()) {
                log.warn("No roles found in token for user: {}. Loading from user details service.", username);
                // Load the user from the database to get their current roles
                UserDetails userDetails = getUserDetailsService().loadUserByUsername(username);
                authorities = userDetails.getAuthorities();
                log.info("Loaded authorities from user details service: {}", authorities);
            } else {
                authorities = Arrays.stream(rolesString.split(","))
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
                log.debug("Using roles from token for user {}: {}", username, rolesString);
            }
            
            return new UsernamePasswordAuthenticationToken(username, null, authorities);
        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate both access and refresh tokens for a user
     */
    private TokenResponse generateTokenResponse(UserDetails userDetails) {
        // Get configuration values
        long accessTokenExpirationMs = configProperties.getJwt().getExpirationMs();
        long refreshTokenExpirationMs = configProperties.getJwt().getRefreshExpirationMs();
        
        // Access token expires according to configuration
        Date accessTokenExpirationDate = new Date(System.currentTimeMillis() + accessTokenExpirationMs);
        
        // Refresh token valid for configured time
        Date refreshTokenExpirationDate = new Date(System.currentTimeMillis() + refreshTokenExpirationMs);
        
        String username = userDetails.getUsername();
        
        // Extract roles
        String roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.joining(","));
        
        // Log roles for debugging purposes
        log.debug("Creating token with roles: {}", roles);
        
        // Generate tokens with roles
        String accessToken = generateToken(username, roles, accessTokenExpirationDate, getAccessTokenKey());
        String refreshToken = generateToken(username, roles, refreshTokenExpirationDate, getRefreshTokenKey());
        
        TokenResponse response = new TokenResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresAt(LocalDateTime.ofInstant(
                accessTokenExpirationDate.toInstant(), ZoneId.systemDefault()));
        response.setUsername(username);
        response.setRoles(roles.isEmpty() ? new String[0] : roles.split(","));
        
        // Add recommended security headers to the response
        addSecurityHeaders(response);
        
        return response;
    }
    
    /**
     * Add security headers to the token response
     */
    private void addSecurityHeaders(TokenResponse response) {
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Content-Security-Policy", 
                "default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'");
        securityHeaders.put("X-Content-Type-Options", "nosniff");
        securityHeaders.put("X-Frame-Options", "DENY");
        securityHeaders.put("X-XSS-Protection", "1; mode=block");
        securityHeaders.put("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        securityHeaders.put("Cache-Control", "no-store, max-age=0");
        securityHeaders.put("Pragma", "no-cache");
        
        response.setSecurityHeaders(securityHeaders);
    }
    
    /**
     * Generate a JWT token
     */
    private String generateToken(String username, String roles, Date expirationDate, Key key) {
        // Ensure roles are included when they are available
        JwtBuilder jwtBuilder = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .setIssuer(configProperties.getJwt().getIssuer());
                
        // Only add roles claim if roles are provided
        if (roles != null && !roles.isEmpty()) {
            jwtBuilder.claim("roles", roles);
        }
        
        return jwtBuilder.signWith(key, SignatureAlgorithm.HS512).compact();
    }
    
    /**
     * Extract all claims from a token
     */
    private Claims extractAllClaims(String token, Key key) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Get the expiration time in milliseconds for a token
     */
    private long getExpirationFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token, getAccessTokenKey());
            return claims.getExpiration().getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            try {
                // Try with refresh token key if access token key fails
                Claims claims = extractAllClaims(token, getRefreshTokenKey());
                return claims.getExpiration().getTime() - System.currentTimeMillis();
            } catch (Exception ex) {
                // If we can't determine expiration, set a default of 24 hours
                log.warn("Could not determine token expiration, using default expiry of 24h");
                return TimeUnit.HOURS.toMillis(24);
            }
        }
    }
    
    /**
     * Get the key for signing access tokens
     */
    private Key getAccessTokenKey() {
        return Keys.hmacShaKeyFor(
                configProperties.getJwt().getSecretKey().getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Get the key for signing refresh tokens
     * Uses a different key to enhance security
     */
    private Key getRefreshTokenKey() {
        // Append "-refresh" to create a different key for refresh tokens
        String refreshKey = configProperties.getJwt().getSecretKey() + "-refresh";
        return Keys.hmacShaKeyFor(refreshKey.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Check if a token is blacklisted
     */
    private boolean isTokenBlacklisted(String token) {
        Long expiryTime = tokenBlacklist.get(token);
        if (expiryTime == null) {
            return false;
        }
        
        // If token is expired, remove it from blacklist
        if (expiryTime < System.currentTimeMillis()) {
            tokenBlacklist.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * Add a token to the blacklist
     * @param token The token to blacklist
     * @param expiryMillis Time in milliseconds until the token can be removed from the blacklist
     */
    private void blacklistToken(String token, long expiryMillis) {
        // Use positive expiry time, minimum 1 minute
        long expiry = Math.max(expiryMillis, TimeUnit.MINUTES.toMillis(1));
        
        // Store expiry time for the token
        tokenBlacklist.put(token, System.currentTimeMillis() + expiry);
        log.debug("Token blacklisted with expiry of {}ms", expiry);
    }
    
    /**
     * Clean up expired tokens from the blacklist
     */
    private void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        int sizeBefore = tokenBlacklist.size();
        
        tokenBlacklist.entrySet().removeIf(entry -> entry.getValue() < now);
        
        int removed = sizeBefore - tokenBlacklist.size();
        if (removed > 0) {
            log.debug("Cleaned up {} expired tokens from blacklist", removed);
        }
    }
    
    /**
     * Helper method to get UserDetailsService and break circular dependency
     */
    private UserDetailsService getUserDetailsService() {
        return userDetailsServiceSupplier.get();
    }
}