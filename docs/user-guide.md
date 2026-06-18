# User Guide

## Purpose

This guide covers day-to-day operation of `react-bff-gateway`: running it from IntelliJ, running the Docker Compose stack, generating a local JWT, calling the API, using Swagger UI, exporting OpenAPI, and finding generated quality reports.

It is intended for engineers evaluating or running the project locally. The README stays focused on the portfolio overview; this guide holds the operational detail.

## Prerequisites

- Java 21
- Maven Wrapper from this repository
- Docker
- Docker Compose
- IntelliJ IDEA, if running from the IDE

## Running From IntelliJ

1. Import the repository as a Maven project.
2. Use JDK 21 for the project SDK.
3. Create a Spring Boot run configuration for `com.dani.bff.ReactBffGatewayApplication`.
4. Set the active profile to `local`.
5. Start the mock downstream services:

```powershell
docker compose up user-service product-service
```

6. Run the Spring Boot application from IntelliJ.
7. Confirm the BFF is healthy:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
```

The BFF listens on `http://localhost:8080` by default.

## Running With Docker Compose

Start the complete local stack:

```bash
docker compose up --build
```

The stack starts:

- BFF: `http://localhost:8080`
- WireMock user service: `http://localhost:8081`
- WireMock product service: `http://localhost:8082`

Stop the stack:

```bash
docker compose down
```

Mock downstream endpoints:

- `GET http://localhost:8081/users/user-123`
- `GET http://localhost:8082/users/user-123/recommendations`

The Compose BFF runs with the `local` profile so Swagger UI and OpenAPI endpoints are enabled.

## Local JWT

The helper script creates an HS256 token suitable for local development:

```powershell
$token = powershell -ExecutionPolicy Bypass -File .\scripts\create-local-jwt.ps1
```

Default claims:

- subject: `user-123`
- issuer: `react-bff-gateway-local`
- audience: `react-dashboard`
- scope: `dashboard:read`
- TTL: 60 minutes

You can override common values:

```powershell
$token = powershell -ExecutionPolicy Bypass -File .\scripts\create-local-jwt.ps1 -Subject user-456 -TtlMinutes 30
```

This local HMAC setup is for development only. Production-style deployments should configure `BFF_JWT_JWK_SET_URI` or `BFF_JWT_ISSUER_URI`.

## Calling The API

Request:

```powershell
Invoke-WebRequest `
  -Headers @{ Authorization = "Bearer $token" } `
  http://localhost:8080/api/dashboard
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

Authentication failures and access-denied responses use the structured `ApiError` model.

## Swagger UI

Swagger UI is available when documentation is enabled, which the `local` profile does automatically:

```text
http://localhost:8080/swagger-ui.html
```

Use the Authorize button with the local JWT token. Enter the token as a Bearer token in Swagger UI, then call `GET /api/dashboard`.

Swagger UI is provided by `springdoc-openapi-starter-webflux-ui`; there is no custom committed Swagger HTML page.

## OpenAPI

Local OpenAPI endpoints:

```text
http://localhost:8080/v3/api-docs
http://localhost:8080/v3/api-docs.yaml
```

Export the specs manually:

```powershell
New-Item -ItemType Directory -Force -Path target\openapi | Out-Null
Invoke-WebRequest -UseBasicParsing http://localhost:8080/v3/api-docs -OutFile target\openapi\openapi.json
Invoke-WebRequest -UseBasicParsing http://localhost:8080/v3/api-docs.yaml -OutFile target\openapi\openapi.yaml
```

CI exports the same contract from the running BFF, uploads it as an artifact, and publishes it under GitHub Pages as:

- `api/openapi.json`
- `api/openapi.yaml`
- `swagger-ui/index.html`

Generated OpenAPI files belong under `target` or CI artifacts, not source control.

## Testing

Run the full verification build:

```bash
./mvnw clean verify
```

The suite covers:

- authenticated dashboard access
- unauthenticated dashboard rejection
- public health endpoint access
- denied-by-default behavior
- OpenAPI JSON/YAML generation and Bearer JWT metadata
- local Swagger UI availability when documentation is enabled
- dashboard aggregation
- user and product downstream success
- downstream failure fallbacks
- circuit-breaker open-state behavior
- DTO serialization and deserialization
- structured error responses
- package boundaries with ArchUnit

Testcontainers is intentionally not used because the project has no database, broker, or external containerized dependency where it would add useful signal.

## Coverage

JaCoCo runs during `verify` and writes HTML to:

```text
target/site/jacoco/index.html
```

Current bundle gates:

- instruction coverage: 70%
- branch coverage: 50%

Generated coverage artifacts are ignored and must not be committed.

## Javadoc

Generate Javadoc locally:

```bash
./mvnw javadoc:javadoc
```

Open:

```text
target/site/apidocs/index.html
```

Javadoc uses Java 21 and is also generated in CI.

## GitHub Pages

GitHub Pages is assembled during CI from generated artifacts and the single maintained landing page template.

Pages content:

- `index.html`: generated from `.github/pages/index.html`
- `javadoc/`: generated Javadoc
- `coverage/`: generated JaCoCo HTML report
- `api/openapi.json`: generated OpenAPI JSON
- `api/openapi.yaml`: generated OpenAPI YAML
- `swagger-ui/`: generated static Swagger UI assets backed by `api/openapi.json`

Repository Pages settings should use GitHub Actions as the Pages source.

## Configuration

| Variable | Purpose | Default |
| --- | --- | --- |
| `SERVER_PORT` | BFF HTTP port | `8080` |
| `USER_SERVICE_BASE_URL` | User service base URL | `http://localhost:8081` |
| `PRODUCT_SERVICE_BASE_URL` | Product service base URL | `http://localhost:8082` |
| `USER_SERVICE_TIMEOUT` | User service timeout | `2s` |
| `PRODUCT_SERVICE_TIMEOUT` | Product service timeout | `2s` |
| `BFF_JWT_JWK_SET_URI` | JWK set URL for production-style JWT validation | empty |
| `BFF_JWT_ISSUER_URI` | OIDC issuer discovery URL for production-style JWT validation | empty |
| `BFF_JWT_SECRET` | Local HS256 secret when no JWK or issuer URI is supplied | local development secret |
| `BFF_JWT_ISSUER` | Optional expected JWT issuer | empty, set in Docker Compose |
| `BFF_JWT_AUDIENCE` | Expected JWT audience | `react-dashboard` |
| `SPRINGDOC_API_DOCS_ENABLED` | Enables OpenAPI JSON/YAML endpoints | `false`, enabled by `local` profile |
| `SPRINGDOC_SWAGGER_UI_ENABLED` | Enables Springdoc Swagger UI | `false`, enabled by `local` profile |

JWT decoder precedence is JWK set URI, then issuer URI, then local HMAC secret.

## Troubleshooting

### Dashboard returns 401

Generate a new token with `scripts/create-local-jwt.ps1`. Confirm the app uses the `local` profile or matching `BFF_JWT_SECRET`, `BFF_JWT_ISSUER`, and `BFF_JWT_AUDIENCE` values.

### Dashboard returns an empty product list

Check whether the product service is running on `http://localhost:8082` or whether the Compose service `product-service` is healthy. The BFF intentionally falls back to an empty recommendation list when product calls fail.

### Dashboard returns Guest User

Check whether the user service is running on `http://localhost:8081` or whether the Compose service `user-service` is healthy. The BFF intentionally falls back to a safe placeholder user when user calls fail.

### Swagger UI is unavailable

Swagger UI is disabled in the default profile. Run with `SPRING_PROFILES_ACTIVE=local` or set `SPRINGDOC_SWAGGER_UI_ENABLED=true`.

### OpenAPI endpoints are unavailable

OpenAPI endpoints are disabled in the default profile. Run with `SPRING_PROFILES_ACTIVE=local` or set `SPRINGDOC_API_DOCS_ENABLED=true`.

### Port 8080, 8081, or 8082 is already in use

Stop the process using the port, or adjust Compose port mappings and the matching downstream base URL environment variables.
