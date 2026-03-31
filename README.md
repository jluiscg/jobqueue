# JobQueue

A distributed task queue and job scheduler built with Java 21 and Spring Boot.

## Features

- Asynchronous job execution with worker pools
- PostgreSQL persistence with ACID transactions
- Redis-based event notifications with polling fallback
- REST API for job submission and management
- Real-time dashboard with WebSocket updates
- Comprehensive metrics and health monitoring

## Tech Stack

- **Backend**: Java 21, Spring Boot 4
- **Database**: PostgreSQL 18
- **Cache**: Redis 8
- **Frontend**: React + TypeScript + Vite
- **Build**: Maven
- **Migrations**: Flyway

## Quick Start

### Prerequisites

- Java 21
- Docker & Docker Compose
- Maven (or use included `./mvnw`)

### Setup

1. Start infrastructure:
   ```bash
   docker compose up -d
   ```

2. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

3. Access the API at `http://localhost:8080`

## Development

- API documentation: `http://localhost:8080/swagger-ui.html`
- Health checks: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/prometheus`

## License

TBD