# URL Shortener — Frontend

A vanilla JavaScript frontend for the URL Shortener API.

## Tech Stack

- HTML5
- CSS3
- Vanilla JavaScript (ES6+)

## Folder Structure

```
frontend/
├── index.html    — Main page layout
├── style.css     — Styling (gradient background, card layout)
└── app.js        — API calls and DOM interactions
```

## Architecture / Workflow

```
User enters URL → Click "Shorten" → POST /shorten → Display short URL
                                                         │
                                                    Click "Copy"
                                                    Clipboard API
```

## How to Run

Open `index.html` in a browser. The backend must be running on `http://localhost:8080`.

No build tools or dependencies required.
