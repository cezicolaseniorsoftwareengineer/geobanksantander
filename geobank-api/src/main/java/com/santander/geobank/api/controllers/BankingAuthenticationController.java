package com.santander.geobank.api.controllers;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.santander.geobank.infrastructure.security.SimpleBankingJwtService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Banking Authentication Controller implementing enterprise security standards.
 *
 * Features:
 * - Real JWT token generation with RSA256 signatures
 * - Correlation ID tracking for audit compliance
 * - Multi-level authorization (admin, operator, viewer)
 * - Banking-grade credential validation
 * - PCI DSS compliant logging (no sensitive data)
 *
 * Security Levels:
 * - admin: Full system access (branch:read,write,admin)
 * - operator: Branch operations (branch:read,write)
 * - viewer: Read-only access (branch:read)
 *
 * @author Banking Security Engineering Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
public class BankingAuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(BankingAuthenticationController.class);

    private final SimpleBankingJwtService jwtService;

    public BankingAuthenticationController(SimpleBankingJwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Banking user authentication endpoint.
     * Generates secure JWT tokens with proper claims and audit trail.
     *
     * @param request login credentials
     * @return JWT token with banking authorities or 401 if invalid
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> authenticateUser(
            @Valid @RequestBody BankingLoginRequest request) {

        String correlationId = UUID.randomUUID().toString();

        logger.info("Authentication attempt for user: {} [correlation: {}]",
                request.username(), correlationId);

        // Validate banking credentials
        BankingUserProfile userProfile = validateBankingCredentials(request.username(), request.password());
        if (userProfile == null) {
            logger.warn("Authentication failed for user: {} [correlation: {}]",
                    request.username(), correlationId);
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "error", "Invalid banking credentials",
                            "correlationId", correlationId));
        }

        // Generate secure banking JWT
        String token = jwtService.generateToken(
                userProfile.username(),
                userProfile.authorities());

        logger.info("Authentication successful for user: {} with authorities: {} [correlation: {}]",
                userProfile.username(), userProfile.authorities(), correlationId);

        // Return banking-compliant token response
        return ResponseEntity.ok(Map.of(
                "access_token", token,
                "token_type", "Bearer",
                "expires_in", 900, // 15 minutes for banking operations
                "scope", String.join(" ", userProfile.authorities()),
                "user_level", userProfile.securityLevel(),
                "correlationId", correlationId,
                "issued_at", System.currentTimeMillis() / 1000));
    }

    /**
     * Validate banking user credentials against secure credential store.
     * In production, this would integrate with LDAP/Active Directory.
     *
     * @param username banking username
     * @param password banking password
     * @return user profile with authorities if valid, null otherwise
     */
    private BankingUserProfile validateBankingCredentials(String username, String password) {

        // Banking credential validation (production would use secure credential store)
        return switch (username) {
            case "admin" -> {
                if ("geobank123".equals(password)) {
                    yield new BankingUserProfile(
                            "admin",
                            Set.of("branch:read", "branch:write", "branch:admin", "system:monitor"),
                            "ADMIN");
                }
                yield null;
            }
            case "operator" -> {
                if ("operator123".equals(password)) {
                    yield new BankingUserProfile(
                            "operator",
                            Set.of("branch:read", "branch:write"),
                            "OPERATOR");
                }
                yield null;
            }
            case "viewer" -> {
                if ("viewer123".equals(password)) {
                    yield new BankingUserProfile(
                            "viewer",
                            Set.of("branch:read"),
                            "VIEWER");
                }
                yield null;
            }
            default -> null;
        };
    }

    /**
     * Banking login request DTO with validation.
     */
    public record BankingLoginRequest(
            @NotBlank(message = "Username is required for banking authentication") String username,

            @NotBlank(message = "Password is required for banking authentication") String password) {
    }

    /**
     * Internal user profile with banking authorities.
     */
    private record BankingUserProfile(
            String username,
            Set<String> authorities,
            String securityLevel) {
    }
}

