# NTU Lottery Frontend (React + Vite)

This is the frontend for the **NTU Lottery Group Project**.  
It communicates with the Spring Boot backend via `/api/*`.

---

## Tech Stack

- React
- Vite
- Fetch API
- Vite Dev Proxy (`/api` → backend)

---

## Prerequisites

- Node.js **18 / 20 LTS recommended**  
  (Node 22 may cause dependency or Vite plugin issues)
- npm 9+

Check your environment:

```bash
node -v
npm -v
Setup & Run
⚠️ Make sure the backend is running at http://localhost:8080 first.

1) Install dependencies
cd frontend/frontend
npm install

2) Start development server
npm run dev

3) Open in browser
http://localhost:5173
Backend API (Used by Frontend)

