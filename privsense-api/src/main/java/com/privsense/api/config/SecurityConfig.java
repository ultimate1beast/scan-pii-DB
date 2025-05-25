package com.privsense.api.config;

import com.privsense.api.security.JwtAuthenticationFilter;
import com.privsense.api.service.auth.AuthenticationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security configuration for the application.
 * Configures authentication, authorization, and security features.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectProvider<UserDetailsService> userDetailsServiceProvider;
    private final ObjectProvider<AuthenticationService> authenticationServiceProvider;
    private final PasswordEncoder passwordEncoder;
    
    // Environment to determine if we're running in production
    @Autowired
    private Environment environment;

    @Autowired
    public SecurityConfig(@Qualifier("userDetailsService") ObjectProvider<UserDetailsService> userDetailsServiceProvider,
                          ObjectProvider<AuthenticationService> authenticationServiceProvider,
                          PasswordEncoder passwordEncoder) {
        this.userDetailsServiceProvider = userDetailsServiceProvider;
        this.authenticationServiceProvider = authenticationServiceProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthFilter = new JwtAuthenticationFilter(authenticationServiceProvider.getObject());
        
        // Determine if we're in production mode
        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        
        var httpSecurity = http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));
                
        // Only disable CSRF in non-production environments
        if (!isProduction) {
            httpSecurity.csrf(csrf -> csrf.disable());
        } else {
            // In production, enable CSRF protection with proper configuration
            httpSecurity.csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/v1/auth/login", "/api/v1/auth/register"));
        }
        
        return httpSecurity                .authorizeHttpRequests(auth -> auth
                        // Public endpoints accessible without authentication
                        .requestMatchers("/api/v1/auth/login").permitAll() // Login endpoint
                        .requestMatchers("/api/v1/auth/register").permitAll() // Public registration endpoint
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swagger UI 
                        .requestMatchers("/actuator/health").permitAll() // Health checks
                        
                        // WebSocket endpoints - allow all for real-time monitoring
                        .requestMatchers("/websocket/**").permitAll() // WebSocket handshake endpoint
                        .requestMatchers("/websocket/info").permitAll() // SockJS info endpoint
                        .requestMatchers("/websocket/iframe.html").permitAll() // SockJS iframe
                        .requestMatchers("/topic/**").permitAll() // WebSocket message topics
                        .requestMatchers("/app/**").permitAll() // WebSocket application destinations
                        
                        // System and user management endpoints restricted to ADMIN role only
                        .requestMatchers("/api/v1/system/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        
                        // Connection management endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/connections/**").hasRole("ADMIN") // Creating and modifying connections
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/connections/**").hasRole("ADMIN") // Closing connections
                        
                        // Allow reading connection data for both roles
                        .requestMatchers(HttpMethod.GET, "/api/v1/connections/**").hasAnyRole("ADMIN", "API_USER")
                        
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsServiceProvider.getObject());
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // In production, specify explicit origins
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        // ✅ Allow credentials for WebSocket connections
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}