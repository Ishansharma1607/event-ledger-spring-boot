# Evidence

Runtime screenshots captured from the local Docker Compose deployment.

- `gateway-swagger-ui.png`: Event Gateway Swagger UI at `http://localhost:8080/swagger-ui.html`
- `account-service-swagger-ui.png`: Account Service Swagger UI at `http://localhost:8081/swagger-ui.html`
- `api-output-evidence.png`: expected API outputs captured from live Gateway and Account Service HTTP responses
- `api-output-evidence.json`: raw request/response evidence for the same API-output run
- `api-output-evidence.html`: browser-readable version of the same API-output run

These screenshots are intended as reviewer evidence that both services expose browsable OpenAPI documentation and return the expected API results for the core event and account flows.
