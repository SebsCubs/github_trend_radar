# Quick Start Guide

## Prerequisites

1. **Docker and Docker Compose** installed
   ```bash
   docker --version
   docker compose version
   ```

2. **GitHub Personal Access Token** in `.env` file
   ```
   GITHUB_TOKEN=your_token_here
   ```
   In production, use a secrets management system (AWS Secrets Manager, Vault, etc.).

3. **Java 21** (only needed for local development, not for Docker)

## Running the Full Stack

### Build and start everything

```bash
docker compose build
docker compose up
```

### Run in detached mode (background)

```bash
docker compose up -d

# Follow all service logs
docker compose logs -f

# Follow a specific service
docker compose logs -f ingestor_service
docker compose logs -f processor_service
```

### Run only specific services

```bash
# Infrastructure only
docker compose up zookeeper kafka kafdrop redis postgres

# Infrastructure + ingestor only (no processor)
docker compose up zookeeper kafka kafdrop redis postgres ingestor_service

# Everything
docker compose up
```

## Services Overview

| Service              | Port  | Description                                      |
|----------------------|-------|--------------------------------------------------|
| Zookeeper            | 2181  | Required by Kafka                                |
| Kafka                | 9092  | Event streaming backbone                         |
| Kafdrop              | 9000  | Kafka web UI for inspecting topics and messages  |
| Redis                | 6379  | Real-time trending leaderboard (sorted sets)     |
| PostgreSQL           | 5432  | Repository metadata, viral milestones, user watches |
| Ingestor Service     | 8080  | Polls GitHub API, sends events to Kafka          |
| Processor Service    | 8081  | Consumes Kafka events, updates Redis + PostgreSQL|

## Architecture

```
GitHub API
    │  (polled every 1 second, 100 events per page)
    ▼
┌─────────────────┐
│ Ingestor Service│ ──► Kafka (raw-github-events)
│   (port 8080)   │
└─────────────────┘
                          │
                          ▼
                   ┌──────────────────┐
                   │ Processor Service│
                   │   (port 8081)    │
                   └──────┬───────────┘
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
        ┌──────────┐          ┌────────────┐
        │  Redis   │          │ PostgreSQL │
        │ (scores) │          │ (metadata) │
        └──────────┘          └────────────┘
```

**Ingestor Service** polls GitHub's `/events` API every second, filters for high-value events
(WatchEvent, ForkEvent, PullRequestEvent, IssuesEvent), and publishes them to Kafka.

**Processor Service** consumes events from Kafka and:
1. Updates the Redis leaderboard with weighted scores
2. Fetches and stores repository metadata in PostgreSQL (on first sighting)
3. Records viral milestones when a repo crosses score thresholds (50, 100, 500, 1000, 5000)

## Verifying the System

### 1. Check all containers are healthy

```bash
docker compose ps
```

### 2. Health endpoints

```bash
# Ingestor service
curl http://localhost:8080/actuator/health

# Processor service
curl http://localhost:8081/actuator/health
```

Expected: `{"status":"UP"}`

### 3. View Kafka messages (Kafdrop)

Open **http://localhost:9000** in your browser.
- Look for the `raw-github-events` topic
- Click into the topic to see messages flowing

### 4. Check Redis leaderboard

```bash
# Connect to Redis and view top trending repos
docker compose exec redis redis-cli ZREVRANGE trending:repos 0 9 WITHSCORES
```

### 5. Check PostgreSQL data

```bash
# Connect to PostgreSQL
docker compose exec postgres psql -U user -d trend_radar

# View stored repository metadata
SELECT repo_name, primary_language, license, owner FROM repository_metadata LIMIT 10;

# View viral milestones
SELECT repo_name, score_threshold, reached_at FROM viral_milestones ORDER BY reached_at DESC LIMIT 10;

# Exit
\q
```

### 6. Service logs

```bash
# Ingestor: should show events being fetched and sent to Kafka
docker compose logs -f ingestor_service

# Processor: should show events being scored and milestones being recorded
docker compose logs -f processor_service
```

## Stopping the Application

```bash
# Stop all services
docker compose down

# Stop and remove all data (Redis, PostgreSQL volumes)
docker compose down -v
```

## Troubleshooting

### No high-value events appearing

The ingestor filters for WatchEvent, ForkEvent, PullRequestEvent, and IssuesEvent.
These are less frequent than PushEvents. Check the logs to verify events are being fetched:
```bash
docker compose logs -f ingestor_service | grep "High value"
```

### Processor not consuming events

```bash
# Check processor logs for errors
docker compose logs processor_service

# Verify Kafka topic has messages via Kafdrop
# Open http://localhost:9000 and check "raw-github-events" topic
```

### Service fails to start

```bash
# Check the failing service logs
docker compose logs <service_name>

# Common issues:
# - Missing GITHUB_TOKEN in .env file
# - Port conflict (another process using 8080, 8081, 9092, etc.)
# - Kafka not ready yet (wait for healthcheck)
```

### Database connection errors

```bash
# Check PostgreSQL is running
docker compose ps postgres

# Check PostgreSQL logs
docker compose logs postgres

# Verify tables were created
docker compose exec postgres psql -U user -d trend_radar -c "\dt"
```
