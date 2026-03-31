# Play IME Preset Dashboard (Scala)

A reactive REST API server for managing IME indicator clock presets, built with **Play Framework 3.0** and **Scala 2.13**.

Connects to a Supabase PostgreSQL database (same schema as [IME Simulator](https://obott9.github.io/ime-simulator/)). Demonstrates functional programming patterns, type-safe queries, and Pekko Streams for reactive data delivery.

## Tech Stack

- **Play Framework 3.0.10** (Pekko-based)
- **Scala 2.13**
- **Slick 3.5** (Functional Relational Mapping)
- **Pekko Streams** (Reactive streaming)
- **PostgreSQL** (Supabase)
- **sbt** (Build tool)

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

### 1. Create Supabase Project

1. Create a free project at [supabase.com](https://supabase.com)
2. Open **SQL Editor** and run `supabase-setup.sql` to create tables and seed data

### 2. Configure Environment

Copy `.env.example` to `.env` and fill in your Supabase credentials:

```bash
cp .env.example .env
```

To find your credentials:
1. Open your Supabase project dashboard
2. Click **Connect** (top bar)
3. Select **Direct** tab > **Session pooler**
4. Copy `host`, `port`, and `user` values
5. DB password is what you set when creating the project (can be reset in Database Settings)

### 3. Run

```bash
export $(cat .env | xargs) && sbt run
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

# Popular presets
curl http://localhost:9000/api/presets/popular?limit=5
```

## Java Version

See [play-ime-preset-api](https://github.com/obott9/play-ime-preset-api) for the Java 21 + Ebean ORM version.

## Support

If you find this project useful:

[![GitHub Stars](https://img.shields.io/github/stars/obott9/play-ime-preset-dashboard?style=social)](https://github.com/obott9/play-ime-preset-dashboard)
[![GitHub Sponsors](https://img.shields.io/badge/Sponsor-GitHub%20Sponsors-ea4aaa)](https://github.com/sponsors/obott9)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-donate-yellow)](https://buymeacoffee.com/obott9)
