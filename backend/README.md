# Budget Tracker — Backend

Spring Boot 3.3 REST API. See the [root README](../README.md) for full setup instructions, architecture, and API reference.

## Quick start

```bash
export DB_URL=jdbc:postgresql://localhost:5432/budget_tracker
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
export JWT_SECRET=any-long-base64-random-string

./gradlew bootRun
```

API runs at `http://localhost:8080`.  
Swagger UI at `http://localhost:8080/swagger-ui.html`.

## Running tests

```bash
./gradlew test
```

Report: `build/reports/tests/test/index.html`
