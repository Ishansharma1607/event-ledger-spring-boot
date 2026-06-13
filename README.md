# Event Ledger

Java 21 / Spring Boot implementation of the Event Ledger take-home assignment.

The system has two independently runnable services:

- `event-gateway`: public API that validates events, enforces idempotency, stores event records, propagates trace IDs, and calls Account Service.
- `account-service`: internal API that owns account balances and transaction history in its own database.

Both services use separate embedded H2 databases and communicate over synchronous REST.

## Requirements

- Java 21
- Git
- Docker Desktop, optional but recommended
- IntelliJ IDEA, optional but recommended

Maven does not need to be installed. This repository includes Maven Wrapper.

## IntelliJ Import

1. Open IntelliJ IDEA.
2. Select `File > Open`.
3. Open the repository root folder.
4. IntelliJ should detect the root `pom.xml` as a Maven multi-module project.
5. Use Java 21 as the project SDK.

Modules:

- `account-service`
- `event-gateway`

## Assessment Checklist

| Requirement | Implementation |
| --- | --- |
| Two services | `event-gateway` and `account-service` are separate Spring Boot applications. |
| Separate databases | Each service owns its own embedded H2 database. |
| Event submission | `POST /events` validates, traces, persists, and forwards events. |
| Account ownership | Account Service owns balances and transaction history. |
| Idempotency | Duplicate `eventId` submissions do not double-apply balance changes. |
| Out-of-order events | Event and transaction listings sort by `eventTimestamp`, then `eventId`. |
| Trace propagation | `X-Trace-ID` is generated, returned, logged, and propagated downstream. |
| Graceful degradation | Gateway returns structured `503` when Account Service is unavailable while local reads stay available. |
| Resiliency | Gateway uses timeout, retry, and circuit breaker around downstream calls. |
| Observability | JSON logs, health endpoints, actuator metrics, and custom counters are included. |
| Local execution | Maven Wrapper and Docker Compose are included. |
| Verification | Unit, web, integration, client, configuration, and Docker smoke paths are covered. |

## Run Tests

```powershell
.\mvnw.cmd test
```

## Build Jars

```powershell
.\mvnw.cmd package
```

## Run Locally Without Docker

Open two terminals.

Terminal 1:

```powershell
.\mvnw.cmd -pl account-service spring-boot:run
```

Terminal 2:

```powershell
.\mvnw.cmd -pl event-gateway spring-boot:run
```

Default ports:

- Gateway: `http://localhost:8080`
- Account Service: `http://localhost:8081`

## Run With Docker Compose

```powershell
docker compose up --build
```

## Main API Examples

Submit an event:

```powershell
curl.exe -i -X POST http://localhost:8080/events `
  -H "Content-Type: application/json" `
  -H "X-Trace-ID: trace-demo-001" `
  -d "{\"eventId\":\"evt-001\",\"accountId\":\"acct-123\",\"type\":\"CREDIT\",\"amount\":150.00,\"currency\":\"USD\",\"eventTimestamp\":\"2026-05-15T14:02:11Z\",\"metadata\":{\"source\":\"mainframe-batch\",\"batchId\":\"B-9042\"}}"
```

Read one event:

```powershell
curl.exe -i http://localhost:8080/events/evt-001
```

List account events in event-time order:

```powershell
curl.exe -i "http://localhost:8080/events?account=acct-123"
```

Read account balance directly from Account Service:

```powershell
curl.exe -i http://localhost:8081/accounts/acct-123/balance
```

## Resiliency Choice

The Gateway protects its Account Service call with:

- timeout through the configured HTTP client
- retry with exponential backoff for transient failures
- circuit breaker to fail fast during sustained Account Service outages

When Account Service is unavailable, `POST /events` returns `503 Service Unavailable` with a structured error body. Gateway-local reads such as `GET /events/{id}` and `GET /events?account=...` continue to work because they only depend on the Gateway database.

## Observability

Every request receives or reuses an `X-Trace-ID`.

- If the client sends `X-Trace-ID`, both services reuse it.
- If absent, the service generates one.
- The Gateway propagates the trace ID to Account Service.
- JSON logs include `service` and MDC fields such as `traceId`.

Health:

- `GET http://localhost:8080/health`
- `GET http://localhost:8081/health`
- `GET http://localhost:8080/actuator/health`
- `GET http://localhost:8081/actuator/health`

Custom metrics:

- `event_gateway_events_accepted_total`
- `event_gateway_events_duplicate_total`
- `event_gateway_account_service_failures_total`
- `account_service_transactions_applied_total`
- `account_service_transactions_duplicate_total`

Example:

```powershell
curl.exe http://localhost:8080/actuator/metrics/event_gateway_events_accepted_total
```

## Notes On Idempotency And Ordering

- Gateway idempotency is keyed by `eventId`.
- Account Service transaction idempotency is also keyed by `eventId`.
- Duplicate event submissions return the original event and do not apply balance changes twice.
- Event listings are ordered by `eventTimestamp`, then `eventId` for deterministic ordering.
- Balance is computed by applying CREDIT as positive and DEBIT as negative, independent of arrival order.

## AI Usage

AI assistance was used to accelerate design, implementation, test generation, and documentation. Engineering choices, validation, and final verification were performed through runnable tests and local builds.
