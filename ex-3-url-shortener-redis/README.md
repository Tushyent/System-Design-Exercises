# Exercise 3 — URL Shortener with Redis Caching

A URL shortening service with Redis caching (cache-aside strategy), built with Spring Boot, MongoDB, and a vanilla JavaScript frontend.

## Tech Stack

- **Backend:** Java 17, Spring Boot 4.1, Spring Data MongoDB, Spring Data Redis, Lombok, Maven
- **Frontend:** HTML5, CSS3, Vanilla JavaScript
- **Databases:** MongoDB (persistence), Redis (cache)

## Folder Structure

```
ex-3-url-shortener-redis/
├── backend/          — Spring Boot REST API
│   ├── src/main/java/com/example/urlshortener_red/
│   │   ├── UrlshortenerRedApplication.java
│   │   ├── config/RedisConfig.java
│   │   ├── controller/URLController.java
│   │   ├── service/URLService.java
│   │   ├── model/URLModel.java
│   │   ├── repository/URLRepo.java
│   │   └── dto/URLRequest.java, URLResponse.java
│   └── src/main/resources/application.properties
├── frontend/         — Vanilla JS client
│   ├── index.html
│   ├── style.css
│   └── app.js
└── EXP3-URL with Redis.pdf
```

## Architecture / Workflow

```
┌──────────┐     POST /shorten      ┌──────────┐     MongoDB     ┌─────────┐
│          │ ──────────────────────> │          │ ─────────────> │         │
│ Frontend │     GET /{shortCode}    │ Backend  │     Redis      │ MongoDB │
│ (HTML/JS)│ <────────────────────── │ (Spring) │ <────────────> │         │
│          │     GET /details/...    │   Boot   │                └─────────┘
└──────────┘                         └──────────┘
                                          │
                                     ┌────┴────┐
                                     │  Redis  │
                                     │  Cache  │
                                     └─────────┘
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/shorten` | Shorten a long URL |
| GET | `/{shortCode}` | Redirect to the original URL |
| GET | `/details/{shortCode}` | Get cache details (hit/miss, response time) |

## How to Run

### Prerequisites

- Java 17+
- MongoDB running on `localhost:27017`
- Redis running on `localhost:6379`

### Backend

```powershell
cd backend
./mvnw spring-boot:run
```

### Frontend

Open `frontend/index.html` in a browser.
