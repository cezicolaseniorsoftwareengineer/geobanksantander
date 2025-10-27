# Architecture Documentation

## System Overview

GeoBank API implements Clean Architecture principles with hexagonal design patterns, ensuring separation of concerns and maintainable enterprise-grade code structure.

## Architectural Layers

### API Layer (Adapters)

**Location:** `src/main/java/com/santander/geobank/api/`

**Responsibilities:**

- HTTP request/response handling
- Input validation and sanitization
- Data transfer object (DTO) transformations
- REST endpoint definitions

**Key Components:**

- `DesafioController`: REST endpoints for geospatial operations
- Request/Response DTOs with validation annotations
- Global exception handling and error response formatting

### Application Layer (Use Cases)

**Location:** `src/main/java/com/santander/geobank/application/`

**Responsibilities:**

- Business workflow orchestration
- Transaction boundary management
- Cross-cutting concern coordination
- Use case implementations

**Key Components:**

- Use case interfaces defining business operations
- Service layer coordinating domain operations
- Application-specific DTOs for data flow

### Domain Layer (Core Business Logic)

**Location:** `src/main/java/com/santander/geobank/domain/`

**Responsibilities:**

- Business rule enforcement
- Domain entity definitions
- Value object implementations
- Core business logic

**Key Components:**

- `Branch`: Domain entity representing bank branch
- `GeoPoint`: Value object for geographic coordinates
- `Distance`: Value object for distance measurements
- Domain services for complex business operations

### Infrastructure Layer (Technical Concerns)

**Location:** `src/main/java/com/santander/geobank/infrastructure/`

**Responsibilities:**

- External system integration
- Database persistence
- Configuration management
- Technical infrastructure

**Key Components:**

- Repository implementations
- Database entity mappings
- Configuration classes
- External service adapters

## Design Patterns

### Repository Pattern

Abstracts data access logic and provides clean interface for domain operations.

```java
public interface BranchRepository {
    Branch save(Branch branch);
    Optional<Branch> findById(BranchId id);
    List<Branch> findWithinRadius(GeoPoint location, double radiusKm);
}
```

### Value Objects

Immutable objects representing domain concepts with built-in validation.

```java
public record GeoPoint(double latitude, double longitude) {
    public GeoPoint {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Invalid latitude");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Invalid longitude");
        }
    }
}
```

### Factory Methods

Controlled object creation with validation and business rules.

```java
public static GeoPoint of(String latitude, String longitude) {
    try {
        return new GeoPoint(
            Double.parseDouble(latitude),
            Double.parseDouble(longitude)
        );
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid coordinate format", e);
    }
}
```

## Dependency Flow

```text
API Layer → Application Layer → Domain Layer ← Infrastructure Layer
```

**Dependency Rules:**

- Inner layers never depend on outer layers
- Dependencies point inward toward domain
- Infrastructure implements domain interfaces
- Application coordinates between layers

## Key Architectural Decisions

### Immutable Value Objects

All value objects are immutable to ensure thread safety and prevent accidental mutations.

**Rationale:**

- Thread-safe by design
- Prevents accidental state changes
- Simplifies reasoning about code behavior
- Enables safe sharing across components

### Constructor Validation

All domain objects validate their state during construction.

**Rationale:**

- Fail-fast principle implementation
- Prevents invalid objects from existing
- Clear error messages at creation point
- Reduces defensive programming needs

### Interface Segregation

Small, focused interfaces rather than large, monolithic ones.

**Rationale:**

- Easier to test and mock
- Clearer dependencies
- Better separation of concerns
- Reduced coupling between components

## Error Handling Strategy

### Exception Hierarchy

Structured exception handling with appropriate abstraction levels.

```java
// Domain exceptions
public class InvalidCoordinateException extends DomainException
public class BranchNotFoundException extends DomainException

// Application exceptions
public class BranchRegistrationException extends ApplicationException

// Infrastructure exceptions
public class DatabaseConnectionException extends InfrastructureException
```

### Error Response Structure

Consistent error format across all endpoints.

```json
{
  "error": {
    "code": "INVALID_COORDINATES",
    "message": "Human-readable error message",
    "details": {
      "field": "latitude",
      "value": "invalid_value",
      "constraint": "must be between -90.0 and 90.0"
    }
  },
  "timestamp": "2025-10-25T10:30:00Z",
  "requestId": "uuid-v4"
}
```

## Testing Strategy

### Unit Tests

Test individual components in isolation with mocked dependencies.

**Coverage:**

- All domain logic
- Value object validation
- Business rule enforcement
- Edge case handling

### Integration Tests

Test component interaction and system behavior.

**Coverage:**

- API endpoint functionality
- Database operations
- Error handling flows
- Cross-layer integration

### Contract Tests

Ensure API contract compliance and backward compatibility.

**Coverage:**

- Request/response formats
- HTTP status codes
- Error response structures
- API versioning

## Performance Considerations

### Database Optimization

- Geospatial indexing for coordinate queries
- Connection pooling with HikariCP
- Prepared statement caching
- Query result pagination

### Calculation Efficiency

- Haversine formula implementation
- Coordinate validation caching
- Distance calculation optimization
- Memory-efficient data structures

### Caching Strategy

- Application-level caching for frequent queries
- Database query result caching
- Coordinate validation result caching
- Cache invalidation strategies

## Security Implementation

### Input Validation

- All boundaries protected with validation
- Coordinate range checking
- Data type validation
- Business rule validation

### Error Information

- No sensitive data in error responses
- Structured logging without exposure
- Audit trail for all operations
- Proper exception sanitization

### Data Protection

- Coordinate precision limiting
- Geographic boundary enforcement
- Input sanitization at entry points
- Proper error message construction
