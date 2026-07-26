# MongoDB Horizontal Sharding — Lab Exercise 4

**UCS3513 System Design Laboratory** — Sri Sivasubramaniya Nadar College of Engineering

Deploy a MongoDB sharded cluster using Docker Compose, configure horizontal sharding on a `Student` collection, and compare performance against a standalone MongoDB instance.

## Architecture

```
 Client / mongosh
       |
  ┌────┴────┐
  │  mongos  │  ← Query Router (port 27017)
  └────┬────┘
       |
  ┌────┴────────┐
  │ Config Server │  ← Metadata (port 27019)
  └─────────────┘
       |
  ┌────┴────┐   ┌────┴────┐
  │ shard1  │   │ shard2  │  ← Data shards (ports 27018, 27028)
  └─────────┘   └─────────┘

Standalone (port 27030) — used only for Task 8 comparison
```

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 24+
- MongoDB Compass (optional, for GUI inspection)

## Project Structure

```
EX4/
├── docker-compose.yml           # Multi-container MongoDB cluster
├── run_lab.ps1                  # PowerShell orchestration (Windows)
├── run_lab.sh                   # Bash orchestration (Linux/macOS)
├── analysis_answers.md          # Q&A for the 4 analysis questions
├── scripts/
│   ├── init-config.js           # Config Server replica set init
│   ├── init-shard1.js           # Shard 1 replica set init
│   ├── init-shard2.js           # Shard 2 replica set init
│   ├── init-router.js           # Add shards, enable sharding, shard collection
│   ├── seed-data.js             # Insert 20 sample student documents
│   ├── verify-lab.js            # sh.status(), queries, distribution
│   └── standalone-comparison.js # Performance comparison (Task 8)
└── Docs/
    ├── EXP4-Sharding-ques.pdf   # Assignment question paper
    └── Lab4_Sharding_Report-inputs.docx  # Sample report with screenshots
```

## Quick Start

```bash
# 1. Start all containers
docker compose up -d

# 2. Run the full lab (PowerShell)
.\run_lab.ps1

# 2. Run the full lab (Bash)
chmod +x run_lab.sh && ./run_lab.sh
```

The script automatically:
1. Waits for all MongoDB instances to be healthy
2. Initialises replica sets (configsvr, shard1RS, shard2RS)
3. Adds shards to the cluster
4. Enables sharding on the `College` database
5. Shards `College.Student` on `{ RollNo: 1 }` with chunk pre-split
6. Seeds 20 student documents
7. Runs verification queries with `explain("executionStats")`
8. Compares sharded vs standalone performance

## Tasks Covered

| Task | Description |
|------|-------------|
| 1 | Configure Config Server, Shard Servers, and mongos |
| 2 | Add shard servers to cluster |
| 3 | Enable sharding for `College` database |
| 4 | Shard `Student` collection on `RollNo` |
| 5 | Display shard distribution (`sh.status()`, `getShardDistribution()`) |
| 6 | Execute search queries via mongos |
| 7 | Analyse document distribution across shards |
| 8 | Compare sharded vs standalone performance |

## Shard Key Design

- **Key**: `{ RollNo: 1 }` (range-based)
- **Chunks**: Pre-split at `RollNo: 10`, upper chunk moved to `shard2RS`
- **Tradeoff**: Range-based sharding on a monotonically increasing key can cause insert hotspots in production. A hashed shard key (`{ RollNo: "hashed" }`) avoids this at the cost of slower range queries.

## Example Commands

```javascript
// Connect via mongos
docker exec -it mongos mongosh --port 27017

// Check shard status
sh.status()

// Per-shard distribution
db.Student.getShardDistribution()

// Targeted shard-key query (SINGLE_SHARD)
db.Student.find({ RollNo: { $gte: 2, $lte: 4 } }).explain("executionStats")

// Non-shard-key query (SHARD_MERGE)
db.Student.find({ Department: "Computer Science" }).explain("executionStats")
```

## Clean Up

```bash
docker compose down -v
```

## Analysis Questions

See [`analysis_answers.md`](analysis_answers.md) for:
1. Role of Config Server, Shard Server, and mongos Router
2. Justification for RollNo as shard key (with tradeoffs)
3. What happens when additional shards are added
4. Standalone vs sharded cluster comparison
