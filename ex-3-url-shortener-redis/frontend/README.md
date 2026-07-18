# URL Shortener with Redis — Frontend

A vanilla JavaScript frontend for the URL Shortener with Redis API. Displays cache hit/miss status and response time.

## Tech Stack

- HTML5
- CSS3
- Vanilla JavaScript (ES6+)

## Folder Structure

```
frontend/
├── index.html    — Main page layout (includes cache info section)
├── style.css     — Styling (gradient background, card layout)
└── app.js        — API calls, DOM interactions, cache details display
```

## Architecture / Workflow

```
User enters URL → Click "Shorten" → POST /shorten → Display short URL
                                                         │
                                                    GET /details/{shortCode}
                                                         │
                                              Show cache status & response time
                                                         │
                                                    Click "Copy"
                                                    Clipboard API
```

## How to Run

Open `index.html` in a browser. The backend must be running on `http://localhost:8080`.

No build tools or dependencies required.
