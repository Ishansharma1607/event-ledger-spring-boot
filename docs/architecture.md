# Event Ledger Architecture

## Overview

```text
Browser / Client
      |
      | REST
      v
Event Gateway API
      |
      | REST with X-Trace-ID
      v
Account Service
```

The services are separate Spring Boot applications with separate H2 databases. They do not share in-process state or database tables.

## Event Gateway

Responsibilities:

- public API for transaction events
- request validation
- event idempotency by `eventId`
- local event storage
- chronological event queries
- trace ID generation and propagation
- resilient REST calls to Account Service
- structured JSON logs and custom metrics

Endpoints:

- `POST /events`
- `GET /events/{id}`
- `GET /events?account={accountId}`
- `GET /health`

## Account Service

Responsibilities:

- account balance ownership
- transaction idempotency by `eventId`
- balance updates inside one transaction
- account detail and balance queries
- structured JSON logs and custom metrics

Endpoints:

- `POST /accounts/{accountId}/transactions`
- `GET /accounts/{accountId}/balance`
- `GET /accounts/{accountId}`
- `GET /health`

## Submit Event Flow

1. Client submits `POST /events` to Gateway.
2. Gateway creates or reuses `X-Trace-ID`.
3. Gateway validates the payload.
4. Gateway checks whether `eventId` already exists locally.
5. Duplicate events return the original event with HTTP 200.
6. New events are sent to Account Service with the same trace ID.
7. Account Service idempotently applies the transaction.
8. Gateway stores the accepted event locally.
9. Gateway returns HTTP 201.

If Account Service is unavailable, the Gateway returns HTTP 503 and does not persist the new event as accepted. The client can safely retry the same `eventId`.

## Data Ownership

Gateway database:

- event records
- original event timestamp
- metadata JSON
- status
- trace ID

Account database:

- accounts
- account balance
- account transactions
- transaction trace ID

## Resiliency

Gateway uses Resilience4j on Account Service calls:

- retry for transient failures
- circuit breaker for sustained failures
- HTTP client timeout to avoid hanging requests

## Observability

Both services expose:

- JSON logs
- trace ID in logs and response headers
- `/health`
- `/actuator/health`
- `/actuator/metrics`
- custom business counters
