# Currency Order Service

A Quarkus-based REST microservice that accepts currency conversion orders, calls a live exchange-rate API, persists orders in PostgreSQL, publishes `ORDER_CREATED` events to Kafka, and indexes them into OpenSearch for live business dashboards.

## Architecture
Client
│
▼
REST API (POST /api/v1/orders)
│
├──► Exchange Rate API (open.er-api.com) — via Quarkus REST Client Reactive
│
├──► PostgreSQL — order persisted via Hibernate ORM Panache
│
└──► Kafka topic "order-created" — ORDER_CREATED event published
│
▼
Kafka Listener (OrderIndexerListener)
│
▼
OpenSearch index "orders"
│
▼
OpenSearch Dashboards (live visualizations)

See `docs/architecture-diagram.png` for the full diagram.

## Tech Stack

- **Java 21**, **Quarkus 3.39.2**
- REST: RESTEasy Reactive + Jackson
- Persistence: Hibernate ORM with Panache, PostgreSQL
- Messaging: SmallRye Reactive Messaging (Kafka)
- External API: Quarkus REST Client Reactive
- Search/Analytics: OpenSearch + OpenSearch Dashboards
- Testing: JUnit 5, Mockito, REST Assured
- Containerization: Docker multi-stage build, Docker Compose

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Docker + Docker Compose

## Project Structure
src/main/java/com/orders/
├── client/ # ExchangeRateClient (REST client), OpenSearchClientProducer
├── dto/ # Request/Response DTOs
├── entity/ # Order (Panache entity)
├── exception/ # Custom exceptions + JAX-RS exception mappers
├── messaging/ # Kafka event payload + OrderIndexerListener
├── resource/ # OrderResource (REST endpoint)
└── service/ # OrderService (business logic)

## Local Setup

### 1. Clone the repository

```bash
git clone https://github.com/sonujha78/Currency-Order-Service.git
cd Currency-Order-Service
```

### 2. Start infrastructure (PostgreSQL, Kafka, OpenSearch, OpenSearch Dashboards)

```bash
docker compose up -d postgres kafka opensearch opensearch-dashboards
```

Wait ~30 seconds for all services to become healthy:

```bash
docker compose ps
```

### 3. Run the application in dev mode

```bash
./mvnw quarkus:dev
```

The app starts on `http://localhost:8080`.

## Running the Full Stack (App + Infra) via Docker

To build and run everything — including the Quarkus app itself — in containers:

```bash
docker compose up -d --build
```

This builds the app using a multi-stage Dockerfile (Maven build stage + lightweight UBI runtime stage) and starts all 5 containers: `orders-postgres`, `orders-kafka`, `orders-opensearch`, `orders-opensearch-dashboards`, `currency-order-service`.

Check status:

```bash
docker compose ps
```

Stop everything:

```bash
docker compose down
```

## API Usage

### Create an order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-1001",
    "amountUSD": 150.00,
    "targetCurrency": "EUR"
  }'
```

**Sample response (201 Created):**

```json
{
  "orderId": 1,
  "customerId": "CUST-1001",
  "amountUSD": 150.00,
  "targetCurrency": "EUR",
  "convertedAmount": 138.50,
  "status": "PROCESSED",
  "createdAt": "2026-09-04T10:00:00Z"
}
```

### Invalid currency example

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-1002",
    "amountUSD": 100.00,
    "targetCurrency": "XYZ"
  }'
```

**Sample response (400 Bad Request):**

```json
{
  "error": "INVALID_CURRENCY",
  "message": "Unsupported or invalid target currency: XYZ",
  "timestamp": "2026-09-04T10:01:00Z"
}
```

## Running Tests

Run the full test suite (6 unit tests + 5 integration tests):

```bash
./mvnw test
```

- **Unit tests** (`OrderServiceTest`) mock the `ExchangeRateClient` with `@InjectMock` and verify business logic: currency conversion, rounding, currency normalization, and error handling.
- **Integration tests** (`OrderResourceTest`) use REST Assured to hit the actual `/api/v1/orders` endpoint end-to-end, verifying HTTP status codes, response bodies, and validation errors.

## OpenSearch Dashboards Setup

The `orders` index is populated automatically as orders are created (via the Kafka → OpenSearch pipeline). Dashboards are provisioned via the OpenSearch Dashboards Saved Objects API.

### 1. Open Dashboards UI
http://localhost:5601

### 2. Import the pre-built dashboard (index pattern + 4 visualizations + dashboard)

```bash
curl -X POST "http://localhost:5601/api/saved_objects/_import?overwrite=true" \
  -H "osd-xsrf: true" \
  --form file=@opensearch-dashboards/saved_objects_export.ndjson
```

### 3. View the dashboard

Navigate to **Dashboards → Currency Order Service - Live Dashboard**.

It includes:

| Visualization | Shows |
|---|---|
| Total Gross Revenue (Metric) | Sum of `amountUSD` across all orders |
| Sales Velocity and Revenue Trend (Line) | Revenue over time, bucketed by `createdAt` |
| Popular Target Currencies (Donut) | Distribution of orders by `targetCurrency` |
| Top 5 VIP Customers (Data Table) | Top 5 `customerId` ranked by total `amountUSD` |

Screenshots of the live dashboard are available in `docs/screenshots/`.

## Configuration

Key settings in `src/main/resources/application.properties`:

| Property | Purpose |
|---|---|
| `quarkus.datasource.jdbc.url` | PostgreSQL connection |
| `quarkus.rest-client.exchange-rate-api.url` | Exchange rate API base URL |
| `kafka.bootstrap.servers` | Kafka broker address |
| `mp.messaging.outgoing.order-created-out.*` | Kafka producer config |
| `mp.messaging.incoming.order-created-in.*` | Kafka consumer config |
| `opensearch.host` / `opensearch.port` / `opensearch.index` | OpenSearch connection |

## Error Handling

The service gracefully handles:

- **Invalid/unsupported target currency** → `400 Bad Request` with `INVALID_CURRENCY` error
- **Exchange rate API unreachable or failing** → `503 Service Unavailable` with `EXTERNAL_API_ERROR`
- **Request validation failures** (missing fields, negative amounts) → `400 Bad Request` via Hibernate Validator
- **OpenSearch indexing failures** are logged but do not fail the original order request (the order is still persisted and the event still published — indexing is best-effort/asynchronous)

## Notes on Docker Networking

- Kafka exposes two listeners: `PLAINTEXT_HOST` (`localhost:9092`, for host-machine tools) and `PLAINTEXT` (`kafka:29092`, for containers on the Docker network). The containerized app uses the internal listener.
- The containerized app runs with `-Djava.net.preferIPv4Stack=true` to avoid IPv6 connectivity issues when calling the external exchange rate API from within Docker's network.
