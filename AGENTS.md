# AGENTS.md

This file guides future Codex and AI coding agents working on `react-bff-gateway`.

## Project Purpose

`react-bff-gateway` is a Java 21 Spring Boot Backend for Frontend for a React dashboard. It exposes one stable `/api/dashboard` endpoint, validates JWTs, aggregates user and product downstream services with `WebClient`, applies Resilience4j circuit-breaker fallbacks, documents the API contract with OpenAPI 3, and publishes generated documentation through GitHub Pages.

The repository is a portfolio project. Keep it credible, small, production-minded, and honest.

## Architecture

The BFF is one deployable Spring Boot WebFlux application.

Package boundaries:

- `api`: HTTP controllers only.
- `config`: Spring configuration, typed configuration properties, security, and WebClient beans.
- `dto`: immutable request/response and downstream mapping records.
- `error`: structured API errors and exception handling.
- `gateway`: downstream HTTP clients and resilience boundary.
- `service`: application orchestration for dashboard aggregation.

Do not split the project into fake microservices. The downstream user and product services are mocked only for local Docker runtime and HTTP-boundary tests.

## Coding Standards

- Use Java 21 language features where they make the code simpler.
- Prefer Java records for DTOs.
- Keep public classes and important public methods documented with useful English Javadoc.
- Keep comments sparse and useful. Do not write comments that repeat method names.
- Keep configuration externalized through `application.yml`.
- Preserve the deny-by-default security posture.
- Do not add Lombok.
- Do not add MapStruct unless mapping complexity clearly justifies generated mapper code.
- Do not add frameworks or abstractions just to make the project look larger.
- Remove dead code, unused methods, unused imports, and obsolete configuration when found.

## Security Expectations

- `/api/**` must require authentication.
- `/actuator/health` and `/actuator/health/**` must remain public.
- All other routes must be denied by default.
- OpenAPI JSON/YAML and Swagger UI must stay disabled by default and enabled intentionally for local/docs generation.
- Authentication and authorization failures must return structured JSON `ApiError` bodies.
- JWT decoder precedence is JWK set URI, issuer URI, then local HMAC secret.
- Tests must not require a real identity provider.
- Do not weaken security defaults without a clear reason and updated tests.

## Resilience Expectations

- Dashboard downstream calls must remain behind Resilience4j circuit breakers.
- User service failures should return a safe placeholder profile.
- Product service failures should return an empty recommendation list.
- The frontend response shape must remain stable when downstreams fail.

## Testing Expectations

Use meaningful tests only. Current test tools are JUnit 5, Spring Boot Test, WebTestClient, Reactor Test, MockWebServer, Spring Security Test, and ArchUnit.

Tests should cover:

- authenticated dashboard access
- unauthenticated dashboard rejection
- public health endpoint access
- denied-by-default behavior
- dashboard aggregation
- user and product client success
- downstream failure and fallback behavior
- circuit breaker open-state behavior
- DTO serialization/deserialization
- structured error responses
- package boundaries
- OpenAPI JSON/YAML generation and Bearer JWT security metadata
- local Swagger UI availability when documentation is enabled

Do not add superficial tests just to raise coverage. Do not add Arquillian. Do not add Testcontainers unless a real external runtime dependency is introduced.

## Coverage and Javadoc

JaCoCo runs during `mvn verify` and writes HTML under `target/site/jacoco`.

Javadoc is generated with:

```bash
./mvnw javadoc:javadoc
```

It writes HTML under `target/site/apidocs`.

Never commit generated coverage or Javadoc output.

## OpenAPI and API Documentation

Springdoc OpenAPI support is provided by `springdoc-openapi-starter-webflux-ui`.

Expectations:

- Keep the documented API surface limited to real BFF endpoints.
- Do not invent endpoints to make the API reference look larger.
- Keep `GET /api/dashboard` documented with summary, description, response examples, structured error examples, and Bearer JWT security.
- Keep `ApiError` and public response DTO schemas useful and aligned with serialization reality.
- Use `springdoc.api-docs.enabled` for OpenAPI JSON/YAML generation.
- Use `springdoc.swagger-ui.enabled` for the standard Springdoc Swagger UI.
- Do not enable docs publicly by default in production-oriented configuration.
- Generated `openapi.json` and `openapi.yaml` belong under `target/openapi` or the CI Pages artifact, not in source control.

Swagger UI is a local development aid at `/swagger-ui.html` when documentation is enabled. It must be provided by Springdoc dependency/configuration, not by a custom committed HTML page. The Pages site links to generated OpenAPI JSON and YAML specs instead of maintaining a duplicate API documentation page.

## Mapping Strategy

MapStruct is intentionally not part of the current project. The existing downstream-to-frontend mappings are small Java record methods with simple defaulting rules. Do not add MapStruct to replace one-line constructors or make the repository look more enterprise-oriented. If mapping complexity becomes real, add it with tests, Maven annotation processing, generated sources under `target/generated-sources`, and updated README/AGENTS guidance.

## CI/CD Expectations

GitHub Actions workflow: `.github/workflows/ci.yml`.

The workflow must:

- run on pushes to `main`
- run on pull requests
- set up Java 21
- cache Maven dependencies
- run validation, tests, packaging, coverage, and Javadoc
- export OpenAPI JSON/YAML from the running BFF
- upload test reports on failure
- upload JaCoCo, Javadoc, and OpenAPI artifacts
- deploy GitHub Pages only from successful pushes to `main`
- use current official GitHub Pages actions

Before changing Pages or Actions behavior, check current official GitHub documentation and avoid deprecated actions or warning-prone syntax.

## GitHub Pages Flow

The only manually maintained landing page is `.github/pages/index.html`.

CI copies generated Javadoc to `target/pages/javadoc`, generated JaCoCo coverage to `target/pages/coverage`, generated OpenAPI specs to `target/pages/api`, substitutes the repository name into the landing page template, uploads the Pages artifact, and deploys with GitHub Pages.

Keep the Pages template accessible, responsive, lightweight, and useful as portfolio documentation. Avoid frontend build chains and duplicated manually maintained documentation pages.

## Docker Expectations

`Dockerfile` builds a production-style runtime image with Java 21.

`docker-compose.yml` starts:

- the BFF on port `8080`
- WireMock user service on port `8081`
- WireMock product service on port `8082`

The Docker setup must not require paid or external services.

## Dependency Rules

Keep dependencies minimal and justified by actual behavior.

Allowed dependency areas:

- Spring Boot WebFlux
- Spring Security OAuth2 Resource Server
- Resilience4j
- Spring Boot Actuator
- Springdoc OpenAPI for WebFlux
- test libraries that validate real behavior
- JaCoCo and Javadoc plugins

Remove unused or duplicated dependencies when found. Do not keep dependencies for resume padding.

## Generated Artifacts

Do not commit:

- `target/`
- `.maven-wrapper-cache/`
- JaCoCo reports
- Javadoc output
- generated OpenAPI specs
- generated mapper implementations
- logs
- IDE files
- Docker local data

The `.gitignore` should continue to protect these paths.

## Validation Before Finishing

Run the relevant commands before handing work back:

```bash
./mvnw clean verify
./mvnw javadoc:javadoc
docker build -t react-bff-gateway:local .
docker compose up --build
```

For Compose validation, confirm the BFF health endpoint and `/api/dashboard` with a local JWT. Stop the stack afterwards with:

```bash
docker compose down
```

Also check:

- GitHub Actions YAML remains valid.
- Pages artifact generation still includes `index.html`, `javadoc/`, and `coverage/`.
- Pages artifact generation includes `api/openapi.json` and `api/openapi.yaml`.
- Swagger UI works locally at `/swagger-ui.html` when documentation is enabled.
- README matches real behavior.
- The landing page matches the implementation and remains the only manually maintained Pages HTML file.
- Generated files are ignored.

## Things To Avoid

- Overengineering.
- Fake enterprise complexity.
- Splitting the BFF into unnecessary services.
- Excessive abstraction.
- Dead code.
- Superficial tests.
- Committing generated reports.
- Weakening coverage gates without justification.
- Weakening security defaults without tests and documentation.
