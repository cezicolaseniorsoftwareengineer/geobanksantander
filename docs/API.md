# API Documentation

## Overview

RESTful API for geospatial banking operations with enterprise-grade validation and error handling.

## Request/Response Patterns

All API endpoints follow consistent patterns for request validation, error handling, and response structure.

### Standard Response Format

```json
{
  "data": {},
  "timestamp": "2025-10-25T10:30:00Z",
  "requestId": "uuid-v4"
}
```

### Error Response Format

```json
{
  "error": {
    "code": "INVALID_COORDINATES",
    "message": "Coordinates must be within valid geographic bounds",
    "details": {
      "field": "latitude",
      "value": "91.0",
      "constraint": "must be between -90.0 and 90.0"
    }
  },
  "timestamp": "2025-10-25T10:30:00Z",
  "requestId": "uuid-v4"
}
```

## Endpoint Specifications

### POST /api/cadastrar-agencia

Register a new branch with geographic coordinates.

**Request Body:**

```json
{
  "latitude": -23.5505,
  "longitude": -46.6333
}
```

**Validation Rules:**

- Latitude: decimal between -90.0 and 90.0
- Longitude: decimal between -180.0 and 180.0
- Brazilian territory validation applied
- Precision: up to 8 decimal places

**Success Response (201 Created):**

```json
{
  "data": {
    "branchId": "550e8400-e29b-41d4-a716-446655440000",
    "latitude": -23.5505,
    "longitude": -46.6333,
    "registered": "2025-10-25T10:30:00Z"
  },
  "timestamp": "2025-10-25T10:30:00Z",
  "requestId": "req-12345"
}
```

**Error Responses:**

*400 Bad Request - Invalid Coordinates:*

```json
{
  "error": {
    "code": "INVALID_COORDINATES",
    "message": "Geographic coordinates are outside valid bounds"
  }
}
```

*400 Bad Request - Outside Brazilian Territory:*

```json
{
  "error": {
    "code": "INVALID_TERRITORY",
    "message": "Coordinates are outside Brazilian territory"
  }
}
```

### POST /api/calcular-distancia

Calculate distance between two geographic points using Haversine formula.

**Request Body:**

```json
{
  "origem": {
    "latitude": -23.5505,
    "longitude": -46.6333
  },
  "destino": {
    "latitude": -23.5489,
    "longitude": -46.6388
  }
}
```

**Success Response (200 OK):**

```json
{
  "data": {
    "distancia": 0.89,
    "unidade": "km",
    "origem": {
      "latitude": -23.5505,
      "longitude": -46.6333
    },
    "destino": {
      "latitude": -23.5489,
      "longitude": -46.6388
    },
    "calculatedAt": "2025-10-25T10:30:00Z"
  },
  "timestamp": "2025-10-25T10:30:00Z",
  "requestId": "req-67890"
}
```

## HTTP Status Codes

- **200 OK**: Successful operation
- **201 Created**: Resource successfully created
- **400 Bad Request**: Invalid input data or constraints violation
- **422 Unprocessable Entity**: Valid input format but business rule violation
- **500 Internal Server Error**: Unexpected server error

## Rate Limiting

Current implementation does not enforce rate limiting. Production deployment should implement appropriate throttling mechanisms.

## Security Headers

All responses include standard security headers:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`

## Coordinate Validation

### Geographic Bounds

- Latitude: -90.0 ≤ lat ≤ 90.0
- Longitude: -180.0 ≤ lon ≤ 180.0

### Brazilian Territory Constraints

- Latitude: -35.0 ≤ lat ≤ 6.0
- Longitude: -75.0 ≤ lon ≤ -30.0

### Precision Handling

- Input precision: up to 8 decimal places
- Calculation precision: double-precision floating point
- Output precision: rounded to 2 decimal places for distance values

## Distance Calculation Algorithm

Implementation uses Haversine formula for great-circle distance calculation:

```text
a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
c = 2 ⋅ atan2(√a, √(1−a))
d = R ⋅ c
```

Where:

- φ is latitude in radians
- λ is longitude in radians
- R is Earth's radius (6,371 km)
- Δφ is the difference in latitude
- Δλ is the difference in longitude

## Testing the API

### Using curl

**Register Branch:**

```bash
curl -X POST http://localhost:8080/api/cadastrar-agencia \
  -H "Content-Type: application/json" \
  -d '{"latitude": -23.5505, "longitude": -46.6333}'
```

**Calculate Distance:**

```bash
curl -X POST http://localhost:8080/api/calcular-distancia \
  -H "Content-Type: application/json" \
  -d '{
    "origem": {"latitude": -23.5505, "longitude": -46.6333},
    "destino": {"latitude": -23.5489, "longitude": -46.6388}
  }'
```
