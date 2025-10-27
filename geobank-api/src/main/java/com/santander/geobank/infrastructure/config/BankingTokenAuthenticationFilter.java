package com.santander.geobank.infrastructure.config;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.santander.geobank.infrastructure.security.SimpleBankingJwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Banking-grade JWT authentication filter implementing enterprise security
 * standards.
 *
 * Security Features:
 * - Bearer token validation with proper JWT parsing
 * - Authority-based access control (RBAC)
 * - Correlation ID extraction for audit trails
 * - PCI DSS compliant request logging
 * - Graceful error handling without information leakage
 *
 * Authorization Levels:
 * - branch:read: View branch information
 * - branch:write: Create/modify branches
 * - branch:admin: Full administrative access
 * - system:monitor: System monitoring access
 *
 * @author Banking Security Engineering Team
 * @since 1.0.0
 */
public class BankingTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(BankingTokenAuthenticationFilter.class);

    private final SimpleBankingJwtService jwtService;

    public BankingTokenAuthenticationFilter(SimpleBankingJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // Validate JWT token
                String username = jwtService.extractUsername(token);
                Set<String> authorities = jwtService.extractAuthorities(token);
                String correlationId = jwtService.extractCorrelationId(token);

                if (username != null && !jwtService.isTokenExpired(token)) {

                    // Create Spring Security authentication
                    List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, grantedAuthorities);

                    // Add correlation ID for audit trail
                    authentication.setDetails(correlationId);

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Add correlation ID to response headers for tracing
                    response.setHeader("X-Correlation-ID", correlationId);

                    logger.debug("Authentication successful for user: {} with authorities: {} [correlation: {}]",
                            username, authorities, correlationId);
                }
            }

        } catch (Exception e) {
            // Security: Don't leak token validation errors
            logger.warn("Authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Skip authentication for public endpoints
        return path.startsWith("/api/v1/auth/") ||
                path.equals("/actuator/health") ||
                path.startsWith("/actuator/info");
    }
}

