# Autocomplete Search System with Trie and Redis — Lab Exercise 7

**UCS3513 System Design Laboratory** — Sri Sivasubramaniya Nadar College of Engineering

A Spring Boot application implementing an autocomplete search system. It uses a custom memory-based **Trie** data structure for prefix matching and integrates **Redis** caching to store frequently queried prefixes and their suggestions.

## What the assignment requires (and where it is)

| Task | Where |
| :--- | :--- |
| Create a dataset containing search terms and frequencies | `src/main/resources/dataset.csv` |
| Develop a Spring Boot application | `src/main/java/com/autocomplete/` |
| Implement a Trie to store and retrieve search terms | `trie/Trie.java`, `trie/TrieNode.java` |
| Return the top-K suggestions based on frequency | `Trie.getTopK()` (DFS + sorting) |
| Integrate Redis to cache frequently requested prefixes | `config/RedisConfig.java`, `service/AutocompleteService.java` |
| Fallback to Trie search on Redis cache miss | `AutocompleteService.getSuggestions()` |
| Store generated suggestions in Redis for subsequent requests | `AutocompleteService.getSuggestions()` (stores with TTL) |
| Test the autocomplete REST API | `scripts/load_test.py` (demo, benchmark, concurrent) |
| Deploy the application and Redis using Docker | `Dockerfile`, `docker-compose.yml` |
| Use JMeter to generate concurrent autocomplete requests | `scripts/Autocomplete_Load_Test.jmx` |

---

## Quick Start

### Automated Orchestration

You can build, deploy, run live demonstrations, and benchmark performance in a single command:

```powershell
# Windows — starts Redis (Docker), builds, runs, executes all benchmarks
.\run_lab.ps1

# Linux/macOS/Git Bash
./run_lab.sh
```

### Manual Execution

1. **Start Redis**:
   ```bash
   docker compose up -d redis
   ```
2. **Run the Spring Boot application**:
   ```bash
   mvn spring-boot:run
   ```
3. **Execute Demos & Load Tests**:
   ```bash
   python scripts/load_test.py demo          # Step-by-step cache verification
   python scripts/load_test.py benchmark     # Latency comparison (Miss vs Hit)
   python scripts/load_test.py concurrent    # Threaded stress test
   ```

*   **Swagger Documentation**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## REST API Endpoints

### 1. GET `/api/autocomplete`
Returns top-K matching suggestions for a given prefix.
*   **Parameters**:
    *   `prefix` (String, default: `""`): The query substring to search.
    *   `limit` (int, default: `5`): Maximum number of suggestions to return.
*   **Response Structure**:
    ```json
    {
      "query": "app",
      "suggestions": [
        { "word": "apple", "frequency": 15000 },
        { "word": "app store", "frequency": 9500 },
        { "word": "application", "frequency": 12000 }
      ],
      "source": "CACHE",
      "executionTimeMs": 0
    }
    ```

### 2. POST `/api/autocomplete/search`
Simulates a search click. Increments a term's search frequency in the Trie and evicts all its prefixes from Redis cache.
*   **Request Body**:
    ```json
    {
      "word": "apple"
    }
    ```

---

## Design and Caching Strategy

1. **Memory-Based Trie**: The vocabulary dataset `dataset.csv` is loaded at startup into a thread-safe Trie structure utilizing `ConcurrentHashMap` children pointers.
2. **Optimal Cache Keys**: Cache keys are structured as `autocomplete:cache:<prefix>`. The server retrieves up to `20` matching suggestions and caches them. Subsequent requests with smaller limits (e.g. `limit=5`) read the cached block and slice it in memory, maximizing cache hits.
3. **Cache Invalidation**: When a user submits a search query (e.g., searching for `"apple"`), the system increments the frequency in the Trie and evicts all prefix keys (`"a"`, `"ap"`, `"app"`, `"appl"`, `"apple"`, and `""`) from Redis to maintain real-time ranking consistency.
4. **Resiliency**: If Redis goes down, the application logs a warning and falls back to Trie search directly, ensuring the API remains available.

---

## Expected Terminal Logs

When the Spring Boot application is running, all core operations output detailed tracking logs directly to the command-line console. This allows you to inspect cache mechanics and Trie interactions in real time:

1. **System Startup (Dataset Loading)**:
   ```
   INFO 1412 --- [restartedMain] com.autocomplete.service.TrieService    : Loading autocomplete dataset from: classpath:dataset.csv
   INFO 1412 --- [restartedMain] com.autocomplete.service.TrieService    : Successfully loaded 74 terms into the Trie.
   ```
2. **First Search Query (Cache Miss -> Fetch Trie & Save Cache)**:
   ```
   INFO 1412 --- [nio-8080-exec-1] c.a.service.AutocompleteService         : Cache miss for prefix: 'app'. Stored 20 results in Redis.
   ```
3. **Subsequent Search Query (Cache Hit -> Sub-millisecond Response)**:
   ```
   INFO 1412 --- [nio-8080-exec-2] c.a.service.AutocompleteService         : Cache hit for prefix: 'app'
   ```
4. **Submitting a Search Word (Trie Update -> Prefix Eviction)**:
   ```
   INFO 1412 --- [nio-8080-exec-3] c.a.service.AutocompleteService         : Processing search submission for word: 'apple'
   INFO 1412 --- [nio-8080-exec-3] c.a.service.AutocompleteService         : Evicted 6 cache keys from Redis for word: 'apple'
   ```

---

## Configuration (`application.properties`)

| Property | Default | Description |
| :--- | :--- | :--- |
| `spring.data.redis.host` | `localhost` | Redis server hostname (overridden to `redis` in Docker) |
| `spring.data.redis.port` | `6379` | Redis server port |
| `server.port` | `8080` | Application port |
| `autocomplete.dataset.path` | `classpath:dataset.csv` | Path to load the initial dataset |
| `autocomplete.cache.ttl-minutes` | `10` | Redis cache Time-To-Live in minutes |

---

## Detailed Analysis

For the complete analysis answering all the lab questions (mechanics of Trie search, role of components, cache workflows, and complexity comparison with linear search), see [`docs/analysis.md`](docs/analysis.md).
