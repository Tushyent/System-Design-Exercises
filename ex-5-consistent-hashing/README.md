# Application-Level Consistent Hashing — Lab Exercise 5

**UCS3513 System Design Laboratory** — Sri Sivasubramaniya Nadar College of Engineering

A Spring Boot application that implements **application-level Consistent Hashing**
to distribute `Student` records (`College.Student`: rollNo, name, dept, year)
across multiple **independent** MongoDB instances deployed with Docker Compose.

This exercise is the consistent-hashing counterpart to
[ex-4-sharding](../ex-4-sharding/) (MongoDB's native sharded cluster). Here the
**application** owns the hash ring and routing logic; each MongoDB instance is a
standalone `mongod` with no config server and no `mongos`.

## Architecture

```
                    Spring Boot App
   ┌──────────────────────────────────────────────┐
   │  ConsistentHashRing  (TreeMap<Long,Node>)    │
   │  150 virtual nodes per physical node         │
   │  SHA-256(rollNo) -> ring position -> owner   │
   └─────────────┬────────────────────────────────┘
                 │ routes each Student to its owner
        ┌────────┴────────┬───────────┬──────────┐
        ▼                 ▼           ▼          ▼
  mongo1:27017     mongo2:27018   mongo3:27019   mongo4:27020 (offline ring)
   (active)         (active)        (active)        (added at runtime)
   College.Student  College.Student  College.Student  College.Student
```

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 24+
- Java 21 + Maven (only for running the app on the host instead of in Docker)
- MongoDB Compass or mongo-express (optional, for GUI inspection)

## Project Structure

```
ex-5-consistent-hashing/
├── SD_A5.docx                   # Lab report (Ex 5) — cover page + full write-up
├── docker-compose.yml           # 4 standalone mongod nodes + 4 mongo-express + app
├── Dockerfile                   # Multi-stage Spring Boot build
├── pom.xml
├── run_lab.ps1                  # PowerShell orchestration (Windows)
├── run_lab.sh                   # Bash orchestration (Linux/macOS)
├── setup.sh                     # Lightweight: just starts the mongod nodes
├── scripts/
│   └── seed-data.js             # Reference seed script (per single node)
├── src/
│   ├── main/java/com/consistenthashing/
│   │   ├── consistenthash/      # HashFunction, StorageNode, ConsistentHashRing
│   │   ├── storage/             # StudentStore, MongoStudentStore, MongoClientProvider, StudentMapper
│   │   ├── model/Student.java
│   │   ├── dto/                 # StudentDto, CreateStudentRequest, CreateNodeRequest, DistributionReport
│   │   ├── service/             # StudentService, NodeService, DistributionService
│   │   ├── controller/          # StudentController, NodeController, DistributionController, RootController
│   │   ├── exception/           # GlobalExceptionHandler, StudentNotFoundException
│   │   ├── config/              # ConsistentHashingConfig, StudentSeeder
│   │   └── ConsistentHashingApplication.java
│   ├── main/resources/          # application.properties, static/index.html
│   └── test/java/...            # 21 unit tests (no Docker required)
└── docs/
    └── analysis.md              # Written answers to the analysis questions
```

## Quick Start

```bash
# 1. One-command full lab (build + seed + add/remove node demos)
.\run_lab.ps1          # Windows
./run_lab.sh           # Linux/macOS

# OR manually:
./setup.sh                          # start mongo1..mongo4
mvn spring-boot:run                 # run the app on the host (localhost:27017...)
#    UI:     http://localhost:8080/
#    Swagger: http://localhost:8080/swagger-ui.html
```

The `StudentSeeder` CommandLineRunner auto-inserts 20 students (RollNo 1001-1020)
on first launch; each is routed to its owning node by hashing the roll number onto
the virtual-node hash ring.

## Tasks Covered

| Task | Description |
|------|-------------|
| 1 | Implement `HashFunction` (SHA-256 key → 64-bit ring position) |
| 2 | Build the sorted hash ring with virtual nodes (`ConsistentHashRing`) |
| 3 | Route create/get/delete of `Student` records through the ring |
| 4 | Verify even distribution across 3 nodes with 20 seeded records |
| 5 | Add a 4th storage node and confirm only ~1/N records migrate |
| 6 | Remove the node and confirm redistribution is minimal and lossless |
| 7 | Compare modulo hashing vs consistent hashing (see `docs/analysis.md`) |
| 8 | Compare application-level consistent hashing vs MongoDB sharding |

## REST API

### Students
```
POST   /api/students            { rollNo, name, dept, year }  -> 201
GET    /api/students            list all
GET    /api/students/{rollNo}   -> 200 | 404
DELETE /api/students/{rollNo}   -> 204 | 404
```

### Storage nodes (hash ring)
```
GET    /api/nodes               list registered nodes
GET    /api/nodes/ring          virtual-node counts per node
POST   /api/nodes               { host, port }  -> registers + migrates
DELETE /api/nodes/{host}/{port} -> removes + redistributes
```

### Distribution
```
GET /api/distribution          counts, vnode counts, total
GET /api/distribution/counts   node -> record count
```

## Demo: Add a storage node

```bash
# 1. Before adding
curl -s localhost:8080/api/distribution | jq '{total, counts}'

# 2. Add mongo4 (already running on 27020; only its hash-ring placement is new)
curl -s -X POST localhost:8080/api/nodes \
  -H 'Content-Type: application/json' \
  -d '{"host":"mongo4","port":27020}' | jq '{migrated, before, after}'

# 3. Verify only a subset migrated; total unchanged
curl -s localhost:8080/api/distribution | jq '{total, counts}'
```

Expected: with 3 nodes, only ~25% of the 20 seed records move to `mongo4`.

## Demo: Remove a storage node

```bash
curl -s -X DELETE localhost:8080/api/nodes/mongo4/27020 | jq '{migrated, before, after}'
curl -s localhost:8080/api/distribution | jq '{total, counts}'
```

Expected: all records formerly on `mongo4` are redistributed to the remaining 3
nodes; total unchanged; no data loss.

## How routing works

1. `StudentService.createStudent` hashes `rollNo` (SHA-256 → long).
2. `ConsistentHashRing.getNode(rollNo)` finds the first virtual node clockwise on
   the ring (`ceilingKey`, wrapping to the start) → the owning `StorageNode`.
3. The record is written to `College.Student` on that node only.
4. Reads go to the same deterministic owner.

## Configuration (`application.properties`)

| Property | Default | Description |
|----------|---------|-------------|
| `app.nodes` | `localhost:27017,localhost:27018,localhost:27019` | Initial ring nodes |
| `app.virtual-nodes` | `150` | Virtual nodes per physical node |
| `app.mongo4.host` | `localhost` | Where the logical `mongo4` demo node lives |
| `app.mongo4.port` | `27020` | Port for the logical `mongo4` demo node |

To run the app **inside** Docker (uses service names): set
`APP_NODES=mongo1:27017,mongo2:27017,mongo3:27017` (see the `app` service). The
demo's `mongo4` is pre-provisioned but not on the ring; the `app` service also
overrides `APP_MONGO4_HOST=mongo4` and `APP_MONGO4_PORT=27017` so registering it
from inside the container reaches the container-internal port.

## Tests

```bash
mvn test            # 21 unit tests (no Docker required)
```

Unit tests cover: hash-ring determinism/add-remove migration/even distribution,
student mapping, in-memory store, student + node + distribution services.

## Clean Up

```bash
docker compose down -v
```

## Analysis Questions

See [`docs/analysis.md`](docs/analysis.md) for the written answers covering:
1. Role of the components (hash ring, virtual nodes, MongoDB storage node)
2. How consistent hashing determines the storage location of a student record
3. What happens when a new MongoDB storage node is added
4. What happens when an existing MongoDB storage node is removed
5. Modulo-based hashing vs Consistent Hashing
6. Application-level consistent hashing vs MongoDB horizontal sharding
