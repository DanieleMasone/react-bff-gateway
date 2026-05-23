# React BFF Gateway

A production-oriented Backend for Frontend (BFF) for a React application, built with Java 21, Spring Boot WebFlux, WebClient, JWT Resource Server security, Resilience4j, Docker, JaCoCo, Javadoc, and GitHub Pages publishing.

The project demonstrates how a React frontend can talk to one stable API while the BFF handles downstream aggregation, security, error shaping, resilience, and operational concerns.

## Architecture

```text
React App
  |
  | HTTP / JSON + Bearer JWT
  v
React BFF Gateway
  |
  | WebClient
  +--> User Service
  |
  +--> Product Service
```

The BFF is a single Spring Boot application. It exposes frontend-oriented DTOs and hides downstream response shapes, latency, failures, and service locations from the React app.

## Main Endpoint

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

## Tech Stack

- Java 21
- Maven
- Spring Boot WebFlux
- WebClient
- Spring Security OAuth2 Resource Server / JWT
- Resilience4j circuit breakers
- Spring Boot Actuator
- JUnit 5, Spring Boot Test, WebTestClient, Reactor Test
- MockWebServer for downstream HTTP simulation
- ArchUnit for architecture rules
- JaCoCo coverage reports and coverage checks
- Javadoc with Java 21
- Docker and Docker Compose
- GitHub Actions and GitHub Pages

## Project Structure

```text
src/main/java/com/dani/bff
|-- api       HTTP controllers
|-- config    security, WebClient, and configuration properties
|-- dto       immutable API and downstream records
|-- error     structured API errors and exception handling
|-- gateway   downstream WebClient clients and resilience boundary
`-- service   dashboard aggregation orchestration
```

## Local Development From IntelliJ

1. Install JDK 21.
2. Import the repository as a Maven project.
3. Create a Spring Boot run configuration for `ReactBffGatewayApplication`.
4. Set active profile to `local`.
5. Start mock downstream services, or point the app at real local services:

```powershell
docker compose up user-service product-service
```

6. Run the app from IntelliJ.
7. Create a local JWT:

```powershell
$token = powershell -ExecutionPolicy Bypass -File .\scripts\create-local-jwt.ps1
```

8. Call the dashboard endpoint:

```powershell
Invoke-WebRequest `
  -Headers @{ Authorization = "Bearer $token" } `
  http://localhost:8080/api/dashboard
```

## Docker Local Runtime

The Compose stack starts the BFF plus two WireMock downstream services.

```bash
docker compose up --build
```

Create a local JWT and call the API:

```powershell
$token = powershell -ExecutionPolicy Bypass -File .\scripts\create-local-jwt.ps1
Invoke-WebRequest -Headers @{ Authorization = "Bearer $token" } http://localhost:8080/api/dashboard
```

Stop the stack:

```bash
docker compose down
```

The mock services are available on:

- User service: `http://localhost:8081/users/user-123`
- Product service: `http://localhost:8082/users/user-123/recommendations`

## Configuration

Important environment variables:

| Variable | Purpose | Default |
| --- | --- | --- |
| `SERVER_PORT` | BFF HTTP port | `8080` |
| `USER_SERVICE_BASE_URL` | User service base URL | `http://localhost:8081` |
| `PRODUCT_SERVICE_BASE_URL` | Product service base URL | `http://localhost:8082` |
| `BFF_JWT_ISSUER_URI` | OIDC issuer discovery URL for production-style validation | empty |
| `BFF_JWT_JWK_SET_URI` | JWK set URL for production-style validation | empty |
| `BFF_JWT_SECRET` | Local HS256 secret when no JWK settings are supplied | local development secret |
| `BFF_JWT_ISSUER` | Expected JWT issuer for local/JWK validation | `react-bff-gateway-local` |
| `BFF_JWT_AUDIENCE` | Expected JWT audience | `react-dashboard` |

For production, prefer `BFF_JWT_JWK_SET_URI` or `BFF_JWT_ISSUER_URI` and avoid the local shared secret.

## Security Model

- `/api/**` requires a valid Bearer JWT.
- `/actuator/health` and `/actuator/health/**` are public for container and platform health checks.
- All other routes are denied by default.
- Authentication and access-denied responses use a structured JSON error body.
- Tests use Spring Security's mock JWT support and do not require a real identity provider.

## Resilience Model

The dashboard aggregation uses Resilience4j circuit breakers around the user and product downstream calls.

- User service failure returns a safe placeholder profile for the authenticated subject.
- Product service failure returns an empty recommendations list.
- The frontend receives a stable response shape even when one or more downstream services fail.
- Circuit breaker health and events are configured for Actuator, while default HTTP security still only permits health publicly.

## Observability

Configured Actuator endpoints:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/circuitbreakers`
- `/actuator/circuitbreakerevents`

Only health is publicly accessible by default. That keeps the repository aligned with a deny-by-default security posture while leaving operational endpoints configured for environments that choose to expose them behind stronger controls.

## Tests

Run the full verification build:

```bash
./mvnw clean verify
```

The test suite covers:

- dashboard security behavior
- unauthenticated access rejection
- actuator health accessibility
- dashboard aggregation
- downstream client success and failure scenarios
- circuit-breaker fallback and open-state behavior
- DTO serialization/deserialization
- package boundary rules with ArchUnit

Testcontainers is intentionally not used because the project has no database, broker, or external runtime dependency where containers would add meaningful signal. MockWebServer and WireMock cover the HTTP boundary with less moving machinery.

## Coverage

JaCoCo is configured in Maven and runs during `verify`.

```bash
./mvnw clean verify
```

Open the generated report:

```text
target/site/jacoco/index.html
```

The build fails if bundle coverage drops below the configured thresholds:

- instruction coverage: 70%
- branch coverage: 50%

Generated coverage output is ignored and should not be committed.

## Javadoc

Generate Javadoc locally:

```bash
./mvnw javadoc:javadoc
```

Open:

```text
target/site/apidocs/index.html
```

The Maven Javadoc plugin is configured for Java 21 with doclint enabled except for missing-comment enforcement. Generated Javadoc output is ignored and should not be committed.

## CI/CD

The GitHub Actions workflow in `.github/workflows/ci.yml` runs on:

- pushes to `main`
- pull requests

The workflow:

- sets up Java 21 with Maven dependency caching
- runs `validate`
- runs `verify`, including tests, package, JaCoCo report generation, and coverage checks
- generates Javadoc
- uploads test reports when a build fails
- uploads JaCoCo HTML as an artifact
- uploads generated Javadoc as an artifact
- prepares a static GitHub Pages site from generated artifacts
- deploys GitHub Pages only for successful pushes to `main`

## GitHub Pages

The Pages site is generated during CI rather than committed. It includes:

- project overview
- generated Javadoc
- generated JaCoCo coverage report
- link back to the GitHub repository

The workflow uses the current GitHub Pages actions:

- `actions/configure-pages`
- `actions/upload-pages-artifact`
- `actions/deploy-pages`

Repository Pages settings should use GitHub Actions as the Pages source.

## Design Trade-Offs

### Why a BFF

A BFF gives the React app a stable, frontend-oriented API and keeps backend topology, failure handling, and service-specific contracts out of the browser.

### Why WebClient

WebClient is a good fit for concurrent downstream HTTP calls. The application exposes a reactive WebFlux API without forcing unnecessary domain complexity.

### Why Records

DTOs are Java records to keep response contracts immutable, concise, and serialization-friendly.

### Why One Deployable

The project is intentionally a single deployable gateway. Splitting it into more services would add operational cost without improving the portfolio value of the example.

### Why No Lombok

The codebase uses Java records and explicit constructors instead of Lombok to keep the build simple and transparent.
