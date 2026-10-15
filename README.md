# Room Booking

A small room booking app.

| Folder | What | Stack |
| --- | --- | --- |
| [`booking-app/`](booking-app) | REST API | Java 21, Spring Boot 4, Spring Security (GitHub OAuth2), JPA, PostgreSQL |
| [`BookApp/`](BookApp) | Web UI | Vue 3, TypeScript, Vite |

Users sign in with GitHub, pick a room and a day, see who has booked what, and
book free time slots. Overlapping bookings are rejected, and users can only
cancel their own bookings.

## Running locally

### 1. Database

The backend expects PostgreSQL on `localhost:5432` with database, user and
password `abc` (override with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`). Tables are
created automatically on startup.

### 2. GitHub OAuth app

Create one at <https://github.com/settings/developers> → **New OAuth App**:

- **Homepage URL:** `http://localhost:5173`
- **Authorization callback URL:** `http://localhost:5173/login/oauth2/code/github`

Copy the client ID and generate a client secret.

### 3. Backend

```bash
cd booking-app
export GITHUB_CLIENT_ID=your-client-id
export GITHUB_CLIENT_SECRET=your-client-secret
./mvnw spring-boot:run
```

### 4. Frontend

```bash
cd BookApp
npm install
npm run dev
```

Open <http://localhost:5173>.

The Vite dev server proxies `/api/*`, `/oauth2/*` and `/login/oauth2/*` to the
backend on port 8080, so the browser only ever talks to one origin and the
session cookie just works.

## API

All endpoints require a signed-in session. Write requests need the
`X-XSRF-TOKEN` header (copied from the `XSRF-TOKEN` cookie).

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/users/me` | The signed-in user |
| GET | `/users`, `/users/{id}` | |
| GET | `/rooms`, `/rooms/{id}` | |
| POST | `/rooms` | `{ "name": "H100" }` |
| GET | `/bookings?roomId=&userId=` | Both filters optional |
| POST | `/bookings` | `{ "roomId": 1, "start": "2026-10-16T09:00:00", "end": "2026-10-16T10:00:00" }`, booked as the signed-in user |
| DELETE | `/bookings/{id}` | Own bookings only |
| POST | `/logout` | |

Errors come back as `{ "message": "..." }` with status 400, 403, 404 or 409.

## Tests

```bash
cd booking-app && ./mvnw test
```

The tests need the local database; they run in transactions that are rolled back.
