# React BFF Gateway

A production-oriented Backend for Frontend built with Spring Boot and WebClient.

The project demonstrates how to design a backend layer dedicated to a React frontend, aggregating multiple downstream APIs, adapting response contracts, and centralizing security, resilience, and operational concerns.

---

## Why this project exists

Frontend applications should not be forced to understand the shape, latency, failures, and security details of multiple backend systems.

This BFF exposes frontend-oriented APIs and hides backend complexity behind a stable contract.

---

## Main responsibilities

- Aggregate multiple downstream APIs
- Adapt backend data models to React-friendly DTOs
- Centralize JWT-based authentication
- Apply resilience patterns with circuit breakers
- Expose operational endpoints through Spring Boot Actuator
- Provide a Docker-based local runtime

---

## Architecture

```text
React App
   |
   | HTTP / JSON
   v
React BFF Gateway
   |
   | WebClient
   v
Downstream APIs
```

---

## Tech stack

- Java 21
- Spring Boot
- Spring WebFlux
- WebClient
- Spring Security
- OAuth2 Resource Server / JWT
- Resilience4j
- Docker
- Docker Compose
- Maven

---

## Project structure

```text
src/main/java/com/dani/bff
├── api
├── config
├── dto
├── error
└── gateway
```

---

## Main endpoint

```http
GET /api/dashboard
Authorization: Bearer <jwt>
```

Example response:

```json
{
  "user": {
    "id": "user-123",
    "displayName": "Demo User",
    "email": "demo@example.com"
  },
  "recommendedProducts": [
    {
      "id": "prd-001",
      "name": "Premium Account",
      "price": 9.99
    }
  ]
}
```

---

## Resilience

The BFF uses Resilience4j circuit breakers around downstream calls.

If a downstream service is unavailable:

- user data falls back to a safe placeholder
- product recommendations return an empty list
- the frontend still receives a valid response shape

This avoids leaking backend instability directly into the frontend.

---

## Security

The `/api/**` endpoints require a valid JWT.

Public endpoints:

```text
/actuator/health
```

All other endpoints are denied by default.

---

## Local development

Run from IntelliJ IDEA:

```text
Run → Edit Configurations → Spring Boot
Active profile: local
```

Then call:

```http
GET http://localhost:8080/api/dashboard
```

---

## Docker local runtime

Build the application:

```bash
./mvnw clean package
```

Start the local stack:

```bash
docker compose up --build
```

Stop it:

```bash
docker compose down
```

---

## Observability

Available actuator endpoints:

```text
GET /actuator/health
GET /actuator/metrics
GET /actuator/info
```

---

## Design trade-offs

### Why BFF

A BFF is useful when the frontend needs a stable, optimized API that differs from backend domain APIs.

It reduces frontend coupling to backend systems and allows backend evolution without breaking the UI.

### Why WebClient

WebClient provides a modern HTTP client model suitable for concurrent downstream aggregation.

The project does not require a fully reactive domain model, but WebClient is appropriate for non-blocking API composition.

### Why not expose downstream APIs directly

Direct frontend-to-service communication creates coupling, duplicated security logic, inconsistent error handling, and poor control over frontend contracts.

### Why not microservices

This project is intentionally a single deployable gateway. Splitting it further would add operational complexity without clear value.

---

## What this repository demonstrates

- Backend for Frontend pattern
- Frontend-oriented API design
- API aggregation
- JWT-secured endpoints
- Resilient downstream communication
- Docker-based local execution
- Production-aware Spring Boot structure
