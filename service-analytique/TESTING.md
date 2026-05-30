# A-to-Z Integration Testing Guide

This guide explains how to fully test the Analytics Microservice (both automated and manually) using the Docker container ecosystem.

## Prerequisites
- Docker & Docker Compose must be installed.
- No local Java, Maven, Kafka, or MongoDB installations are required. Everything runs inside containers.

---

## 1. Run the Build (JUnit Tests)

Before starting the ecosystem, ensure the code builds successfully by running all tests inside a disposable Maven container:
```powershell
docker run --rm `
  -v "${PWD}:/app" `
  -w /app `
  maven:3.9.6-eclipse-temurin-17 `
  mvn test
```
**Expected Output:** `BUILD SUCCESS` (0 failures, 0 errors).

---

## 2. Start the Environment

Spin up all dependencies (MongoDB, Zookeeper, Kafka, ML Service, Prometheus, Grafana, and Analytics Service) in a unified network.

```powershell
docker compose build
docker compose up -d
docker compose ps
```
Wait for all services to show as `Up (healthy)` or `Started`.

---

## 3. Automated End-to-End Test Suite

1. Start the Docker Compose environment.
2. Ensure you have the `sgitu-internal` network created (the docker-compose file handles this).
3. Run the dashboard seed script to inject realistic historical data (optional, but recommended for visual testing):
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\seed-dashboard-data.ps1
   ```
4. Execute the automated test harness script:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\run-integration-tests.ps1
   ```
*Note: This script will display a clear green/red scorecard detailing the success or failure of each step.*

---

## 4. Manual Verification Steps

If you want to manually test the API via `curl` or Postman, follow these steps:

### Phase 3 - Security & JWT
Without a token, endpoints will correctly block you:
```powershell
curl -i http://localhost:8088/api/v1/analytics/dashboard
```
**Expected:** `401 Unauthorized`

*(To obtain a valid JWT manually for testing, run the `run-integration-tests.ps1` script once to auto-generate a cryptographic JWT in the logs, or use a tool like jwt.io with the secret key `sgitu_g8_secret_key_2025_very_long_secret_for_analytics` to generate an `HS256` token with `sub: admin-agent`, `roles: ["ADMIN"]`.)*

### Phase 4 - Data Ingestion
Ingest a valid Ticketing event (assuming `$token` is your JWT):
```powershell
curl -i -X POST http://localhost:8088/api/v1/ingestion/tickets `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d '[{"schemaVersion":1,"timestamp":"2026-05-30T10:00:00Z","userId":"user-001","status":"validated","line":"L1","stationId":"ST-101"}]'
```
**Expected:** `201 Created`

### Phase 5 - Async Kafka Streaming
Push a mock event directly into the Kafka broker:
```powershell
docker exec -i g8-kafka kafka-console-producer `
  --bootstrap-server localhost:9092 `
  --topic g2-ticketing-events `
  <<EOF
{"schemaVersion":1,"timestamp":"2026-05-30T10:05:00Z","userId":"kafka-stream-user","status":"validated","line":"L2","stationId":"ST-202"}
EOF
```
Verify the analytics service consumed it by reading its logs:
```powershell
docker compose logs g8-analytics-service --tail=20
```

### Phase 6 - ETL & ML Predictions
Manually trigger the scheduled analytics aggregations job:
```powershell
curl -i http://localhost:8088/test/run
```
Retrieve the compiled dashboard metrics:
```powershell
curl -s http://localhost:8088/api/v1/analytics/dashboard `
  -H "Authorization: Bearer $token"
```

## 5. View Grafana Dashboards
Navigate to Grafana in your web browser to see live metrics:
- **URL:** `http://localhost:3000`
- **User:** `admin`
- **Password:** `sgitu2026`
