# Event Ledger Spring Boot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a polished Java 21/Spring Boot Event Ledger assignment with two independently runnable services, strong tests, Docker Compose, and IntelliJ-friendly Maven structure.

**Architecture:** A root Maven project contains two Spring Boot modules: `event-gateway` and `account-service`. The Gateway persists incoming events in its own H2 database and synchronously calls the Account Service through a Resilience4j-protected REST client. The Account Service owns balances and transaction history in its own H2 database.

**Tech Stack:** Java 21, Spring Boot 3.5.15.RELEASE, Maven Wrapper, Spring Web MVC, Spring Data JPA, H2, Bean Validation, Resilience4j, Micrometer/Actuator, Logstash Logback Encoder, JUnit 5, Spring Boot Test, OkHttp MockWebServer, Docker Compose.

---

## File Structure

- Create `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, and `mvnw.cmd` from Spring Initializr output so local Maven is not required.
- Create `pom.xml` as the root Maven parent/aggregator.
- Create `event-gateway/pom.xml` for Gateway dependencies.
- Create `account-service/pom.xml` for Account Service dependencies.
- Create `event-gateway/src/main/java/com/mphasis/eventledger/gateway/...` for Gateway source.
- Create `account-service/src/main/java/com/mphasis/eventledger/account/...` for Account Service source.
- Create module-specific `src/test/java/...` test suites.
- Create module-specific `src/main/resources/application.yml` and `logback-spring.xml`.
- Create `docker-compose.yml`, `README.md`, and `docs/architecture.md`.

---

### Task 1: Maven Scaffolding And Wrapper

**Files:**
- Create: `.mvn/wrapper/maven-wrapper.properties`
- Create: `mvnw`
- Create: `mvnw.cmd`
- Create: `pom.xml`
- Create: `event-gateway/pom.xml`
- Create: `account-service/pom.xml`

- [ ] **Step 1: Add Maven wrapper scripts**

Use Spring Initializr-generated Maven wrapper scripts for Boot `3.5.15.RELEASE`. The wrapper should download Maven automatically so Windows users can run `.\mvnw.cmd test`.

- [ ] **Step 2: Add root Maven parent**

Root `pom.xml` defines modules, Java 21, Spring Boot parent `3.5.15.RELEASE`, and dependency versions for Resilience4j, MockWebServer, and Logstash Logback Encoder.

- [ ] **Step 3: Add module POMs**

`event-gateway/pom.xml` includes web, validation, JPA, H2, actuator, aop, resilience4j, logstash encoder, test, and mockwebserver. `account-service/pom.xml` includes web, validation, JPA, H2, actuator, logstash encoder, and test.

- [ ] **Step 4: Verify Maven project imports**

Run:

```powershell
.\mvnw.cmd -version
```

Expected: Maven downloads if needed and prints Maven, Java 21, and Windows runtime information.

- [ ] **Step 5: Commit**

```powershell
git add .mvn mvnw mvnw.cmd pom.xml event-gateway/pom.xml account-service/pom.xml
git commit -m "build: scaffold multi-module spring boot project"
```

---

### Task 2: Account Service Domain And Tests

**Files:**
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/AccountServiceApplication.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/domain/TransactionType.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/domain/AccountEntity.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/domain/AccountTransactionEntity.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/repository/AccountRepository.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/repository/AccountTransactionRepository.java`
- Create: `account-service/src/test/java/com/mphasis/eventledger/account/AccountTransactionServiceTest.java`

- [ ] **Step 1: Write failing Account Service behavior tests**

Create `AccountTransactionServiceTest` with tests for:

- credit creates an account balance
- debit reduces balance
- duplicate event ID does not double-apply
- account ID mismatch is rejected
- recent transactions are sorted chronologically

- [ ] **Step 2: Run test to verify RED**

Run:

```powershell
.\mvnw.cmd -pl account-service test -Dtest=AccountTransactionServiceTest
```

Expected: compilation fails because service/domain classes do not exist yet.

- [ ] **Step 3: Implement domain entities and repositories**

Add JPA entities with `BigDecimal` money fields, `Instant` timestamps, `eventId` primary key on transactions, and optimistic lock `version` on accounts.

- [ ] **Step 4: Run test to verify GREEN**

Run:

```powershell
.\mvnw.cmd -pl account-service test -Dtest=AccountTransactionServiceTest
```

Expected: tests pass.

- [ ] **Step 5: Commit**

```powershell
git add account-service/src
git commit -m "feat(account): apply idempotent account transactions"
```

---

### Task 3: Account Service REST API

**Files:**
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/api/ApplyTransactionRequest.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/api/AccountResponse.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/api/BalanceResponse.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/api/TransactionResponse.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/api/AccountController.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/api/HealthController.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/error/ApiErrorResponse.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/error/GlobalExceptionHandler.java`
- Create: `account-service/src/test/java/com/mphasis/eventledger/account/AccountControllerTest.java`

- [ ] **Step 1: Write failing MVC tests**

Create tests for:

- `POST /accounts/{accountId}/transactions` returns 201 for a new transaction
- duplicate transaction returns 200 and same balance
- `GET /accounts/{accountId}/balance` returns current balance
- invalid transaction amount returns 400 with `VALIDATION_ERROR`
- `X-Trace-ID` appears in response

- [ ] **Step 2: Run test to verify RED**

```powershell
.\mvnw.cmd -pl account-service test -Dtest=AccountControllerTest
```

Expected: compilation fails because controller and DTO classes do not exist yet.

- [ ] **Step 3: Implement DTOs, controller, health endpoint, and error handler**

Use Java records for DTOs. Use Bean Validation annotations on request records. Include `traceId` in success responses and error responses.

- [ ] **Step 4: Run account-service tests**

```powershell
.\mvnw.cmd -pl account-service test
```

Expected: account-service test suite passes.

- [ ] **Step 5: Commit**

```powershell
git add account-service/src
git commit -m "feat(account): expose account transaction api"
```

---

### Task 4: Gateway Domain And Validation

**Files:**
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/EventGatewayApplication.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/domain/EventEntity.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/domain/EventStatus.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/domain/TransactionType.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/repository/EventRepository.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/api/EventRequest.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/api/EventResponse.java`
- Create: `event-gateway/src/test/java/com/mphasis/eventledger/gateway/EventValidationTest.java`

- [ ] **Step 1: Write failing Gateway validation tests**

Create tests for:

- valid CREDIT request maps to an entity
- zero amount is rejected
- missing account ID is rejected
- unknown type is rejected by JSON binding
- account events are listed by `eventTimestamp`

- [ ] **Step 2: Run test to verify RED**

```powershell
.\mvnw.cmd -pl event-gateway test -Dtest=EventValidationTest
```

Expected: compilation fails because Gateway model classes do not exist.

- [ ] **Step 3: Implement Gateway domain, DTOs, and repository**

Use `BigDecimal`, `Instant`, `@Enumerated(EnumType.STRING)`, and `@Lob` for metadata JSON.

- [ ] **Step 4: Run Gateway validation tests**

```powershell
.\mvnw.cmd -pl event-gateway test -Dtest=EventValidationTest
```

Expected: tests pass.

- [ ] **Step 5: Commit**

```powershell
git add event-gateway/src
git commit -m "feat(gateway): add event persistence model"
```

---

### Task 5: Gateway Account Client With Resiliency

**Files:**
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/client/AccountClient.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/client/AccountTransactionRequest.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/client/AccountClientException.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/config/RestClientConfig.java`
- Create: `event-gateway/src/test/java/com/mphasis/eventledger/gateway/AccountClientTest.java`

- [ ] **Step 1: Write failing AccountClient tests**

Use MockWebServer to prove:

- client sends `X-Trace-ID`
- client posts the expected transaction JSON
- downstream 503 becomes `AccountClientException`
- retry attempts happen for transient 5xx responses

- [ ] **Step 2: Run test to verify RED**

```powershell
.\mvnw.cmd -pl event-gateway test -Dtest=AccountClientTest
```

Expected: compilation fails because client classes do not exist.

- [ ] **Step 3: Implement RestClient-based AccountClient**

Use Spring `RestClient`. Annotate downstream call with Resilience4j `@Retry`, `@CircuitBreaker`, and `@TimeLimiter` only if compatible with synchronous calls; otherwise use `@Retry` and `@CircuitBreaker` plus RestClient timeouts. Convert downstream failures into a stable domain exception.

- [ ] **Step 4: Run AccountClient tests**

```powershell
.\mvnw.cmd -pl event-gateway test -Dtest=AccountClientTest
```

Expected: tests pass and verify retry behavior through MockWebServer request count.

- [ ] **Step 5: Commit**

```powershell
git add event-gateway/src
git commit -m "feat(gateway): add resilient account client"
```

---

### Task 6: Gateway Event API

**Files:**
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/service/EventService.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/api/EventController.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/api/HealthController.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/error/ApiErrorResponse.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/error/GlobalExceptionHandler.java`
- Create: `event-gateway/src/test/java/com/mphasis/eventledger/gateway/EventControllerTest.java`

- [ ] **Step 1: Write failing Gateway controller tests**

Create tests for:

- `POST /events` returns 201 and stores event after Account Service success
- duplicate `eventId` returns 200 and does not call Account Service twice
- `GET /events/{id}` returns stored event
- `GET /events?account=...` returns chronological events
- Account Service outage returns 503 with `ACCOUNT_SERVICE_UNAVAILABLE`
- Gateway-local reads still work after an outage

- [ ] **Step 2: Run test to verify RED**

```powershell
.\mvnw.cmd -pl event-gateway test -Dtest=EventControllerTest
```

Expected: compilation fails because controller/service classes do not exist.

- [ ] **Step 3: Implement EventService, controller, health endpoint, and error handler**

Persist only after Account Service confirms the transaction. Return existing event for duplicate submissions. Include `traceId` in every response.

- [ ] **Step 4: Run Gateway tests**

```powershell
.\mvnw.cmd -pl event-gateway test
```

Expected: gateway test suite passes.

- [ ] **Step 5: Commit**

```powershell
git add event-gateway/src
git commit -m "feat(gateway): expose event ledger api"
```

---

### Task 7: Trace Propagation, Metrics, And Structured Logs

**Files:**
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/observability/TraceFilter.java`
- Create: `event-gateway/src/main/java/com/mphasis/eventledger/gateway/observability/GatewayMetrics.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/observability/TraceFilter.java`
- Create: `account-service/src/main/java/com/mphasis/eventledger/account/observability/AccountMetrics.java`
- Create: `event-gateway/src/main/resources/logback-spring.xml`
- Create: `account-service/src/main/resources/logback-spring.xml`
- Modify: service classes to increment counters
- Create: `event-gateway/src/test/java/com/mphasis/eventledger/gateway/TracePropagationIntegrationTest.java`

- [ ] **Step 1: Write failing trace propagation integration test**

Start both apps in test mode with a mocked Account Service endpoint or in-process server and verify `X-Trace-ID` flows from Gateway to Account Service client request.

- [ ] **Step 2: Run test to verify RED**

```powershell
.\mvnw.cmd -pl event-gateway test -Dtest=TracePropagationIntegrationTest
```

Expected: test fails because trace filter and propagation are incomplete.

- [ ] **Step 3: Implement TraceFilter, metrics components, and JSON logback config**

Use MDC key `traceId`. If `X-Trace-ID` is absent, generate a UUID. Return the trace ID in response headers. Add Micrometer counters for accepted, duplicate, downstream failure, and account transactions applied.

- [ ] **Step 4: Run module tests**

```powershell
.\mvnw.cmd test
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```powershell
git add event-gateway/src account-service/src
git commit -m "feat: add tracing metrics and structured logs"
```

---

### Task 8: Configuration And Docker Compose

**Files:**
- Create: `event-gateway/src/main/resources/application.yml`
- Create: `account-service/src/main/resources/application.yml`
- Create: `event-gateway/Dockerfile`
- Create: `account-service/Dockerfile`
- Create: `docker-compose.yml`

- [ ] **Step 1: Write configuration smoke tests**

Add or extend existing Spring Boot context tests to prove both apps load with their default profiles.

- [ ] **Step 2: Run context tests to verify RED if config is missing**

```powershell
.\mvnw.cmd test
```

Expected before implementation: context load tests fail or are absent.

- [ ] **Step 3: Add application configuration and Dockerfiles**

Gateway default port is `8080`; Account Service default port is `8081`. Docker Compose wires `ACCOUNT_SERVICE_BASE_URL=http://account-service:8081`, maps both ports, and adds health checks.

- [ ] **Step 4: Run tests**

```powershell
.\mvnw.cmd test
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```powershell
git add event-gateway/src/main/resources account-service/src/main/resources event-gateway/Dockerfile account-service/Dockerfile docker-compose.yml
git commit -m "chore: add runtime configuration and docker compose"
```

---

### Task 9: Documentation And Final Verification

**Files:**
- Create: `README.md`
- Create: `docs/architecture.md`
- Optionally create: `docs/api-examples.http`

- [ ] **Step 1: Write README and architecture docs**

Document prerequisites, IntelliJ import, Maven wrapper commands, Docker Compose, local run, API curl examples, tests, resiliency decision, observability, and AI usage.

- [ ] **Step 2: Run full Maven test suite**

```powershell
.\mvnw.cmd test
```

Expected: all tests pass.

- [ ] **Step 3: Build application jars**

```powershell
.\mvnw.cmd package
```

Expected: both services produce executable Spring Boot jars.

- [ ] **Step 4: Run Docker Compose build**

```powershell
docker compose build
```

Expected: both service images build.

- [ ] **Step 5: Commit**

```powershell
git add README.md docs/architecture.md docs/api-examples.http
git commit -m "docs: add usage and architecture guide"
```

---

## Self-Review

- Spec coverage: The plan covers two services, separate H2 databases, idempotency, out-of-order event listing, trace propagation, structured logs, health checks, metrics, resiliency, graceful degradation, Docker Compose, tests, README, and IntelliJ import.
- Placeholder scan: No task uses TBD, TODO, or undefined future work as an implementation substitute.
- Type consistency: Event fields use `eventId`, `accountId`, `type`, `amount`, `currency`, `eventTimestamp`, and `metadata` consistently across DTOs and entities.
