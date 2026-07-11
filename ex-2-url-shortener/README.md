# Exercise 2 — URL Shortener

A URL shortening service built with Spring Boot, MongoDB, and a vanilla JavaScript frontend.

## Tech Stack

- **Backend:** Java 17, Spring Boot 4.1, Spring Data MongoDB, Lombok, Maven
- **Frontend:** HTML5, CSS3, Vanilla JavaScript
- **Database:** MongoDB

## Folder Structure

```
ex-2-url-shortener/
├── backend/          — Spring Boot REST API
│   ├── src/main/java/com/example/urlshortener/
│   │   ├── UrlshortenerApplication.java
│   │   ├── controller/UrlController.java
│   │   ├── service/UrlService.java
│   │   ├── model/UrlModel.java
│   │   ├── repository/UrlRepository.java
│   │   └── util/HashUtil.java
│   └── src/main/resources/application.properties
├── frontend/         — Vanilla JS client
│   ├── index.html
│   ├── style.css
│   └── app.js
└── EXP2-URL.pdf
```

## Architecture / Workflow

```
┌──────────┐     POST /shorten      ┌──────────┐     MongoDB     ┌─────────┐
│          │ ──────────────────────> │          │ ─────────────> │         │
│ Frontend │     GET /{shortCode}    │ Backend  │                │ MongoDB │
│ (HTML/JS)│ <────────────────────── │ (Spring) │ <───────────── │         │
│          │     302 Redirect        │   Boot   │                └─────────┘
└──────────┘                         └──────────┘
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/shorten` | Shorten a long URL |
| GET | `/{shortCode}` | Redirect to the original URL |

## How to Run

### Backend

```powershell
cd backend
./mvnw spring-boot:run
```

Requires MongoDB running on `localhost:27017`.

### Frontend

Open `frontend/index.html` in a browser.
