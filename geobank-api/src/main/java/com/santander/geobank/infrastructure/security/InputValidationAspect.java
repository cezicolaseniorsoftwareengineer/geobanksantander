package com.santander.geobank.infrastructure.security;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Input Validation Aspect for banking-grade data validation.
 * Enforces validation at all API boundaries to prevent injection attacks.
 *
 * Security Patterns:
 * - Bean Validation (JSR-380) enforcement
 * - SQL/NoSQL injection prevention
 * - XSS attack mitigation
 * - Command injection protection
 * - Path traversal prevention
 *
 * Compliance:
 * - OWASP Top 10: A03:2021 - Injection
 * - PCI DSS Requirement 6.5.1: Injection flaws prevention
 * - CWE-20: Improper input validation
 *
 * @author Banking Security Team
 * @since 1.0.0
 */
@Aspect
@Component
public class InputValidationAspect {

    private static final Logger logger = LoggerFactory.getLogger(InputValidationAspect.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final Validator validator;

    public InputValidationAspect(Validator validator) {
        this.validator = validator;
    }

    /**
     * Intercept controller methods to validate input parameters.
     */
    @Around("execution(* com.santander.geobank.api.controllers..*(..))")
    public Object validateControllerInputs(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().toShortString();

        for (Object arg : args) {
            if (arg != null && shouldValidate(arg)) {
                validateObject(arg, methodName);
                performSecurityChecks(arg, methodName);
            }
        }

        return joinPoint.proceed();
    }

    /**
     * Validate object using Bean Validation.
     */
    private void validateObject(Object obj, String context) {
        Set<ConstraintViolation<Object>> violations = validator.validate(obj);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            securityLogger.warn("VALIDATION_FAILURE | context: {} | violations: {}", context, errors);

            throw new IllegalArgumentException("Validation failed: " + errors);
        }
    }

    /**
     * Perform additional security checks on input data.
     */
    private void performSecurityChecks(Object obj, String context) {
        String stringValue = obj.toString();

        // Check for SQL injection patterns
        if (containsSqlInjectionPattern(stringValue)) {
            securityLogger.error("SQL_INJECTION_ATTEMPT | context: {} | value: {}", context, maskSensitiveData(stringValue));
            throw new SecurityException("Potential SQL injection detected");
        }

        // Check for XSS patterns
        if (containsXssPattern(stringValue)) {
            securityLogger.error("XSS_ATTEMPT | context: {} | value: {}", context, maskSensitiveData(stringValue));
            throw new SecurityException("Potential XSS attack detected");
        }

        // Check for path traversal patterns
        if (containsPathTraversalPattern(stringValue)) {
            securityLogger.error("PATH_TRAVERSAL_ATTEMPT | context: {} | value: {}", context, maskSensitiveData(stringValue));
            throw new SecurityException("Potential path traversal detected");
        }

        // Check for command injection patterns
        if (containsCommandInjectionPattern(stringValue)) {
            securityLogger.error("COMMAND_INJECTION_ATTEMPT | context: {} | value: {}", context, maskSensitiveData(stringValue));
            throw new SecurityException("Potential command injection detected");
        }
    }

    /**
     * Determine if object should be validated.
     */
    private boolean shouldValidate(Object obj) {
        return !obj.getClass().getName().startsWith("org.springframework") &&
                !obj.getClass().getName().startsWith("jakarta.servlet");
    }

    /**
     * Detect SQL injection patterns.
     */
    private boolean containsSqlInjectionPattern(String value) {
        if (value == null) return false;
        
        String lowerValue = value.toLowerCase();
        return lowerValue.contains("' or '1'='1") ||
                lowerValue.contains("' or 1=1") ||
                lowerValue.contains("--") ||
                lowerValue.contains("/*") ||
                lowerValue.contains("*/") ||
                lowerValue.contains("xp_") ||
                lowerValue.contains("sp_") ||
                lowerValue.contains("exec(") ||
                lowerValue.contains("execute(") ||
                lowerValue.contains("union select") ||
                lowerValue.contains("drop table") ||
                lowerValue.contains("delete from");
    }

    /**
     * Detect XSS patterns.
     */
    private boolean containsXssPattern(String value) {
        if (value == null) return false;
        
        String lowerValue = value.toLowerCase();
        return lowerValue.contains("<script") ||
                lowerValue.contains("javascript:") ||
                lowerValue.contains("onerror=") ||
                lowerValue.contains("onload=") ||
                lowerValue.contains("onclick=") ||
                lowerValue.contains("<iframe") ||
                lowerValue.contains("eval(");
    }

    /**
     * Detect path traversal patterns.
     */
    private boolean containsPathTraversalPattern(String value) {
        if (value == null) return false;
        
        return value.contains("../") ||
                value.contains("..\\") ||
                value.contains("%2e%2e/") ||
                value.contains("%2e%2e\\");
    }

    /**
     * Detect command injection patterns.
     */
    private boolean containsCommandInjectionPattern(String value) {
        if (value == null) return false;
        
        String lowerValue = value.toLowerCase();
        return lowerValue.contains(";") && (lowerValue.contains("rm ") || lowerValue.contains("del ")) ||
                lowerValue.contains("&&") ||
                lowerValue.contains("||") ||
                lowerValue.contains("|") && (lowerValue.contains("bash") || lowerValue.contains("sh")) ||
                lowerValue.contains("$(") ||
                lowerValue.contains("`");
    }

    /**
     * Mask sensitive data for logging.
     */
    private String maskSensitiveData(String value) {
        if (value == null || value.length() <= 10) {
            return "***MASKED***";
        }
        return value.substring(0, 5) + "..." + value.substring(value.length() - 5);
    }
}
