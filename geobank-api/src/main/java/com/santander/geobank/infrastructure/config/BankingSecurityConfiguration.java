package com.santander.geobank.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.santander.geobank.infrastructure.security.SimpleBankingJwtService;

/**
 * Banking-grade security configuration implementing enterprise standards.
 *
 * Security Features:
 * - Method-level authorization with @PreAuthorize
 * - JWT-based stateless authentication
 * - CORS configuration for banking domains
 * - Security headers for PCI DSS compliance
 * - Role-based access control (RBAC)
 *
 * Authorization Matrix:
 * - branch:read: View branch information and distances
 * - branch:write: Create and modify branch data
 * - branch:admin: Administrative operations
 * - system:monitor: System health and metrics
 *
 * @author Banking Security Engineering Team
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class BankingSecurityConfiguration {

        private final SimpleBankingJwtService jwtService;

        public BankingSecurityConfiguration(SimpleBankingJwtService jwtService) {
                this.jwtService = jwtService;
        }

        @Bean
        public SecurityFilterChain bankingSecurityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Add banking JWT authentication filter
                                .addFilterBefore(
                                                new BankingTokenAuthenticationFilter(jwtService),
                                                UsernamePasswordAuthenticationFilter.class)

                                .authorizeHttpRequests(authz -> authz
                                                // Public endpoints (no authentication required)
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers("/actuator/info").permitAll()

                                                // TEST-ONLY endpoint: public for integration/monitoring
                                                .requestMatchers("/api/v1/desafio/test-only").permitAll()

                                                // Protected endpoints (JWT required - authorization via @PreAuthorize)
                                                .requestMatchers("/api/v1/desafio/**").authenticated()
                                                .requestMatchers("/actuator/**").authenticated()

                                                // Deny all other requests
                                                .anyRequest().denyAll())

                                // Banking CORS configuration
                                .cors(cors -> cors.configurationSource(bankingCorsConfigurationSource()))

                                // Disable CSRF for stateless API
                                .csrf(csrf -> csrf.disable())

                                // Banking security headers
                                .headers(headers -> headers
                                                .frameOptions(frameOptions -> frameOptions.deny())
                                                .contentTypeOptions(contentTypeOptions -> contentTypeOptions.and())
                                                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                                                .maxAgeInSeconds(31536000))
                                                .and())

                                .build();
        }

        /**
         * Banking-compliant CORS configuration.
         * Restricts origins to banking domains for security.
         */
        @Bean
        public org.springframework.web.cors.CorsConfigurationSource bankingCorsConfigurationSource() {
                org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();

                // Banking domains only (production would use actual Santander domains)
                configuration.setAllowedOriginPatterns(java.util.List.of(
                                "http://localhost:*",
                                "https://*.santander.com.br",
                                "https://*.geobank.santander.com.br"));

                configuration.setAllowedMethods(java.util.List.of("GET", "POST", "OPTIONS"));
                configuration.setAllowedHeaders(java.util.List.of("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
