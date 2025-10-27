package com.santander.geobank.infrastructure.security;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT Authentication Filter for banking-grade request validation.
 * Implements PCI DSS compliant token verification and audit trail.
 *
 * Security Features:
 * - Bearer token extraction and validation
 * - Correlation ID propagation for distributed tracing
 * - Authority-based access control
 * - Failed authentication logging for security monitoring
 * - MDC context population for structured logging
 *
 * Compliance:
 * - PCI DSS Requirement 8.2.3: Token-based authentication
 * - PCI DSS Requirement 10.2: Audit trail for authentication events
 * - PSD2 SCA: Strong customer authentication support
 *
 * @author Banking Security Team
 * @since 1.0.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_USER_KEY = "userId";
    private static final String MDC_CORRELATION_KEY = "correlationId";

    private final SimpleBankingJwtService jwtService;

    public JwtAuthenticationFilter(SimpleBankingJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null && jwtService.isTokenValid(token) && !jwtService.isTokenExpired(token)) {
                authenticateUser(token, request);
                auditSuccessfulAuthentication(request, token);
            } else if (token != null) {
                auditFailedAuthentication(request, "Invalid or expired token");
            }

        } catch (Exception e) {
            logger.error("Authentication filter error: {}", e.getMessage());
            auditFailedAuthentication(request, "Filter exception: " + e.getMessage());
        } finally {
            filterChain.doFilter(request, response);
            MDC.clear();
        }
    }

    /**
     * Extract JWT token from Authorization header.
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Authenticate user and populate security context.
     */
    private void authenticateUser(String token, HttpServletRequest request) {
        String username = jwtService.extractUsername(token);
        Set<String> authorities = jwtService.extractAuthorities(token);
        String correlationId = jwtService.extractCorrelationId(token);

        // Populate MDC for structured logging
        MDC.put(MDC_USER_KEY, username);
        MDC.put(MDC_CORRELATION_KEY, correlationId);

        // Add correlation ID to response headers for tracing
        if (request instanceof HttpServletRequest) {
            ((HttpServletResponse) request).setHeader(CORRELATION_ID_HEADER, correlationId);
        }

        // Create authentication token with authorities
        var grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        var authentication = new UsernamePasswordAuthenticationToken(
                username, null, grantedAuthorities);

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        logger.debug("User authenticated: {} with authorities: {} correlation: {}",
                username, authorities, correlationId);
    }

    /**
     * Audit successful authentication for compliance.
     */
    private void auditSuccessfulAuthentication(HttpServletRequest request, String token) {
        String username = jwtService.extractUsername(token);
        String correlationId = jwtService.extractCorrelationId(token);
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        securityLogger.info("AUTH_SUCCESS | user: {} | correlation: {} | ip: {} | uri: {} | agent: {}",
                username, correlationId, ipAddress, request.getRequestURI(), userAgent);
    }

    /**
     * Audit failed authentication for security monitoring.
     */
    private void auditFailedAuthentication(HttpServletRequest request, String reason) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        securityLogger.warn("AUTH_FAILURE | reason: {} | ip: {} | uri: {} | agent: {}",
                reason, ipAddress, request.getRequestURI(), userAgent);
    }

    /**
     * Extract real client IP address considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip authentication for public endpoints
        return path.startsWith("/actuator/health") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/error");
    }
}
