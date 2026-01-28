# Quick Start Guide - Running the Application (Without Processor Service)

## Prerequisites

1. **Docker and Docker Compose** installed
   ```bash
   docker --version
   docker-compose --version
   ```

2. **GitHub Personal Access Token** in `.env` file
   - Your `.env` file should contain: `GITHUB_TOKEN=your_token_here`
   - The token is already in your `.env` file ✅

3. **Java 21** (only if building locally, not needed for Docker)

## Step-by-Step Instructions

### Option 1: Run All Services Except Processor (Recommended)

```bash
# Start all infrastructure + ingestor service (excludes processor_service)
docker-compose up zookeeper kafka kafdrop redis postgres ingestor_service
```

### Option 2: Run in Detached Mode (Background)

```bash
# Start services in background
docker-compose up -d zookeeper kafka kafdrop redis postgres ingestor_service

# View logs
docker-compose logs -f ingestor_service

# Stop all services
docker-compose down
```

### Option 3: Build and Run

```bash
# Build the ingestor service image first
docker-compose build ingestor_service

# Then start all services
docker-compose up zookeeper kafka kafdrop redis postgres ingestor_service
```

## Services That Will Start

1. **Zookeeper** (port 2181) - Required for Kafka
2. **Kafka** (port 9092) - Message broker
3. **Kafdrop** (port 9000) - Kafka UI for viewing messages
4. **Redis** (port 6379) - Not used yet (for processor service)
5. **PostgreSQL** (port 5432) - Not used yet (for processor service)
6. **Ingestor Service** (port 8080) - The main service that fetches GitHub events

## Verifying Everything is Running

### 1. Check Service Health

```bash
# Check all running containers
docker-compose ps

# Check ingestor service health
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### 2. Check Ingestor Service Logs

```bash
# View logs
docker-compose logs -f ingestor_service
```

You should see:
- Service initialization messages
- "Fetching GitHub events from API" every 5 seconds
- Event processing statistics

### 3. View Kafka Messages (Kafdrop UI)

Open in browser: **http://localhost:9000**

- You should see the `raw-github-events` topic
- Messages will appear as the ingestor service fetches events from GitHub

### 4. Check Metrics

```bash
# Application info
curl http://localhost:8080/actuator/info

# Metrics
curl http://localhost:8080/actuator/metrics
```

## Troubleshooting

### Issue: Ingestor service fails to start

**Check:**
```bash
# View detailed logs
docker-compose logs ingestor_service

# Common issues:
# 1. Missing GITHUB_TOKEN - check .env file
# 2. Kafka not ready - wait for Kafka healthcheck to pass
# 3. Build failure - run: docker-compose build ingestor_service
```

### Issue: Cannot connect to Kafka

**Solution:**
```bash
# Wait for Kafka to be healthy
docker-compose ps kafka

# Check Kafka logs
docker-compose logs kafka
```

### Issue: Port already in use

**Solution:**
```bash
# Check what's using the port
lsof -i :8080  # For ingestor service
lsof -i :9092  # For Kafka
lsof -i :9000  # For Kafdrop

# Stop conflicting services or change ports in docker-compose.yml
```

## Stopping the Application

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clears data)
docker-compose down -v
```

## What Happens When Running

1. **Infrastructure starts** (Zookeeper, Kafka, Redis, PostgreSQL)
2. **Ingestor service starts** and connects to Kafka
3. **Every 5 seconds**, the ingestor service:
   - Fetches events from GitHub API
   - Filters for high-value events (WatchEvent, ForkEvent)
   - Sends events to Kafka topic `raw-github-events`
4. **Events accumulate in Kafka** (waiting for processor service to consume them)

## Next Steps

Once the processor service is implemented, you can add it with:
```bash
docker-compose up processor_service
```

Or run everything:
```bash
docker-compose up
```
