# Event Ledger Design

## Goal

Build an original Java/Spring Boot take-home submission for the Event Ledger assignment. The solution should look production-minded, run reliably, be easy to import into IntelliJ IDEA, and demonstrate strong backend engineering judgment without becoming unnecessarily large.

## Technology Choices

- Java 21
- Spring Boot 3.x
- Maven multi-module project
- H2 embedded databases, one per service
- Spring Web, Spring Validation, Spring Data JPA
- Resilience4j for timeout, retry, and circuit breaker
- Micrometer and Spring Boot Actuator for health and metrics
- Logback JSON encoder for structured logs
- JUnit 5, Spring Boot Test, WireMock or MockWebServer for downstream-failure tests
- Docker Compose for running both services

## Repository Layout

```text
event-ledger/
  pom.xml
  docker-compose.yml
  README.md
  event-gateway/
    pom.xml
    src/main/java/...
    src/test/java/...
  account-service/
    pom.xml
    src/main/java/...
    src/test/java/...
  docs/
    architecture.md
```

The root `pom.xml` imports both services as Maven modules so IntelliJ can open the root folder and recognize the full project.

## Service Responsibilities

### Event Gateway

The Gateway is the only public-facing service. It exposes:

- `POST /events`
- `GET /events/{id}`
- `GET /events?account={accountId}`
- `GET /health`
- Actuator health and metrics endpoints

It validates incoming payloads, creates or propagates a trace ID, enforces idempotency using `eventId`, stores accepted events in its own H2 database, and calls the Account Service over synchronous REST. Duplicate event submissions return the original event without applying the transaction again.

Event listings are ordered by normalized event timestamp, then `eventId` as a deterministic tie-breaker.

### Account Service

The Account Service is internal. It exposes:

- `POST /accounts/{accountId}/transactions`
- `GET /accounts/{accountId}/balance`
- `GET /accounts/{accountId}`
- `GET /health`
- Actuator health and metrics endpoints

It stores transactions in its own H2 database, enforces idempotency using `eventId`, and maintains account balance as CREDIT minus DEBIT. Balance updates happen in one database transaction with transaction insertion.

## Data Model

Gateway event:

- `eventId` primary key
- `accountId`
- `type` as `CREDIT` or `DEBIT`
- `amount` as `BigDecimal`
- `currency`
- `eventTimestamp` as `Instant`
- `metadataJson`
- `status`
- `createdAt`
- `traceId`

Account transaction:

- `eventId` primary key
- `accountId`
- `type`
- `amount`
- `currency`
- `eventTimestamp`
- `createdAt`
- `traceId`

Account:

- `accountId` primary key
- `balance` as `BigDecimal`
- `currency`
- `updatedAt`
- optimistic lock `version`

## Request Flow

For `POST /events`:

1. Gateway validates the payload.
2. Gateway checks whether `eventId` already exists.
3. If duplicate, it returns the stored event with HTTP 200.
4. If new, Gateway calls Account Service with the trace ID header.
5. Account Service applies the transaction idempotently.
6. Gateway stores the event locally after downstream success.
7. Gateway returns HTTP 201 with the stored event.

This keeps the default behavior simple: when Account Service is unavailable, the event is not stored as accepted and the client can retry the same `eventId` safely.

## Resiliency

Gateway wraps Account Service calls with:

- short timeout
- retry with exponential backoff for transient transport and 5xx errors
- circuit breaker to fail fast during sustained outages

When Account Service is unavailable, Gateway returns HTTP 503 with a structured error body. Gateway-local reads, such as `GET /events/{id}` and `GET /events?account=...`, continue working.

## Observability

Every request gets a trace ID. If the client sends `X-Trace-ID`, Gateway uses it; otherwise Gateway generates one. Gateway passes it to Account Service via `X-Trace-ID`.

Both services log structured JSON with:

- timestamp
- level
- service name
- trace ID
- message
- relevant IDs such as event ID and account ID

Each service provides:

- `GET /health`
- Spring Boot Actuator health
- metrics through Actuator/Micrometer
- at least one custom metric, such as events accepted, duplicate events, downstream failures, and transactions applied

## Error Handling

All validation and runtime errors return a consistent response:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "amount must be greater than zero"
  },
  "traceId": "..."
}
```

Expected error codes include:

- `VALIDATION_ERROR`
- `EVENT_NOT_FOUND`
- `ACCOUNT_NOT_FOUND`
- `ACCOUNT_ID_MISMATCH`
- `ACCOUNT_SERVICE_UNAVAILABLE`
- `DUPLICATE_TRANSACTION`
- `INTERNAL_ERROR`

## Testing Strategy

Tests must be runnable with:

```bash
mvn test
```

Coverage:

- validation failures
- idempotent duplicate event submission
- out-of-order event listing
- balance correctness across CREDIT and DEBIT
- Account Service transaction idempotency
- Gateway graceful degradation when Account Service is down
- circuit breaker or retry behavior
- trace ID propagation
- one full Gateway to Account Service integration flow

## README Expectations

The README should include:

- architecture overview
- API examples with curl
- IntelliJ import instructions
- Java and Maven prerequisites
- Docker Compose instructions
- local manual run instructions
- test command
- resiliency choice explanation
- observability notes
- AI usage note, written honestly and professionally

## Scope Boundaries

The first implementation will not include authentication, Kafka, service discovery, Kubernetes, or full OpenTelemetry collector setup. Those would add noise for this assignment. The solution will mention them as possible production extensions while delivering the required synchronous REST architecture cleanly.
