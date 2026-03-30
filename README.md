# Play IME Preset Dashboard (Scala)

A reactive REST API server for managing IME indicator clock presets, built with **Play Framework 3.0** and **Scala 2.13**.

Connects to the existing [IME Simulator](https://obott9.github.io/ime-simulator/) Supabase PostgreSQL database. Demonstrates functional programming patterns, type-safe queries, and Pekko Streams for reactive data delivery.

## Tech Stack

- **Play Framework 3.0.10** (Pekko-based)
- **Scala 2.13**
- **Slick 3.5** (Functional Relational Mapping)
- **Pekko Streams** (Reactive streaming)
- **PostgreSQL** (Supabase)
- **sbt 1.10.11** (Build tool)

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/presets` | List presets (paginated) |
| `GET` | `/api/presets/:id` | Get preset by ID |
| `POST` | `/api/presets` | Create preset |
| `PUT` | `/api/presets/:id` | Update preset |
| `DELETE` | `/api/presets/:id` | Delete preset |
| `GET` | `/api/presets/shared/:code` | Get by share code |
| `POST` | `/api/presets/:id/like` | Toggle like |
| `GET` | `/api/presets/popular` | Popular presets |
| `GET` | `/api/presets/stream` | SSE stream (Pekko Streams) |
| `GET` | `/api/health` | Health check |

## Key Scala Features Demonstrated

- **Immutable case classes** for data models
- **Option/Either** for functional error handling (no exceptions)
- **Future-based** async composition
- **Slick FRM** — type-safe, composable database queries
- **Pekko Streams** — backpressure-aware reactive streaming via SSE
- **Pattern matching** for request validation and routing

## Setup

### Prerequisites

- Java 21+
- sbt 1.9+

### Configuration

1. Copy `.env.example` to `.env` and fill in your Supabase credentials:

```bash
cp .env.example .env
```

2. Set environment variables:

```bash
export SUPABASE_DB_HOST=db.xxxxxxxxxxxx.supabase.co
export SUPABASE_DB_PASSWORD=your-password
export SUPABASE_PROJECT_REF=xxxxxxxxxxxx
```

### Run

```bash
sbt run
```

Server starts at `http://localhost:9000`.

### Example Requests

```bash
# Health check
curl http://localhost:9000/api/health

# List presets
curl http://localhost:9000/api/presets

# Stream presets (Server-Sent Events)
curl http://localhost:9000/api/presets/stream

# Create a preset
curl -X POST http://localhost:9000/api/presets \
  -H "Content-Type: application/json" \
  -d '{"name": "My Theme", "settings": {"clock": {"mode": "digital"}}}'
```

## Java Version

See [play-ime-preset-api](https://github.com/obott9/play-ime-preset-api) for the Java 21 + Ebean ORM version of this project.

## Development

This project was developed in collaboration with Claude AI (architecture design, code generation, documentation).

## Support

If you find this project useful:

[![GitHub Stars](https://img.shields.io/github/stars/obott9/play-ime-preset-dashboard?style=social)](https://github.com/obott9/play-ime-preset-dashboard)
[![GitHub Sponsors](https://img.shields.io/badge/Sponsor-GitHub%20Sponsors-ea4aaa)](https://github.com/sponsors/obott9)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-donate-yellow)](https://buymeacoffee.com/obott9)
