# DoURemember — Full‑stack (Angular + Spring Boot + DeepSeek AI/Django)

---

## Table of contents

1. [Overview / Architecture](#overview--architecture)
2. [Prerequisites](#prerequisites)
3. [Quick start — run everything (recommended: one terminal per service)](#quick-start--run-everything-recommended-one-terminal-per-service)
4. [Expose frontend on port 80 (optional)](#expose-frontend-on-port-80-optional)
5. [Configuration notes / environment variables](#configuration-notes--environment-variables)
6. [Troubleshooting](#troubleshooting)
7. [Minimal dev checklist](#minimal-dev-checklist)

---

## Overview / Architecture

High level components in this repo:

* **Frontend** — Angular SPA served with `ng serve`.

  * Entry: `frontend/dourememberfront/src/main.ts`
  * Start script: `frontend/dourememberfront/package.json` (`npm run start`).

* **Backends (Spring Boot)**

  * **Users backend**: `backend/users-backend`

    * Main class: `com.springboot.backend.salazar.usersbackend.users_backend.UsersBackendApplication` (path: `backend/users-backend/src/main/java/.../UsersBackendApplication.java`).
  * **Groundtruth backend**: `backend/groundtruth-backend`

    * Main class: `com.springboot.backend.douremember.groundtruth.groundtruth_backend.GroundtruthBackendApplication` (path: `backend/groundtruth-backend/src/main/java/.../GroundtruthBackendApplication.java`).

* **AI stack (Python + Django listener)**

  * **AI socket server**: `backend/ai/ai_socket/server.py` — standalone Python TCP socket server that talks to the OpenAI/router API.
  * **Optional test client**: `backend/ai/ai_socket/client.py`.
  * **Django "listener"** (REST endpoint that forwards user answers to the AI socket): `backend/ai/ai_socket/userAnswerToGroundTestListener/` (run with `manage.py`).

* **Ground truth flow (example)**: Frontend → `GroundTruthService.sendUserAnswerToAiGroundTruthTest` → Spring controllers/services (`GroundTruthController`, `GroundTruthServiceImpl`) → optionally to Django listener → AI socket.

---

## Prerequisites

* **Java 21** and **Maven** (the repo contains `mvnw` wrappers).
* **Node.js** (>=16) and **npm**.
* **Python 3.10+** and `venv`.
* **MySQL** for the Users backend (or edit `application.properties` to use another DB).
* **OpenAI / router API key** for AI code — add to `backend/ai/ai_socket/.env` as `api_key`.

Files to review for defaults and configuration:

* `backend/users-backend/src/main/resources/application.properties`
* `backend/ai/ai_socket/.env` (create or edit)
* `backend/ai/ai_socket/userAnswerToGroundTestListener/api/views.py` (HOST / PORT used to connect to the socket server)

---

## Quick start — run everything (recommended terminal-per-service)

> Run these commands from the repository root. If you prefer a single script or Docker, see **Troubleshooting & Notes** below.

### 1) Start Users backend (Spring Boot)

```bash
cd backend/users-backend
./mvnw spring-boot:run
```

If you need to change the port for this app:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

Main class entry: `com.springboot.backend.salazar.usersbackend.users_backend.UsersBackendApplication`.

---

### 2) Start Groundtruth backend (Spring Boot)

```bash
cd ../../groundtruth-backend
./mvnw spring-boot:run
```

If the default 8080 port conflicts, run with a different port as shown above (e.g. `--server.port=8081`).

Main class entry: `com.springboot.backend.douremember.groundtruth.groundtruth_backend.GroundtruthBackendApplication`.

---

### 3) Start the Angular frontend

```bash
cd ../../../frontend/dourememberfront
npm install
npm run start
# default dev server: http://localhost:4200
```

Frontend entry: `frontend/dourememberfront/src/main.ts`.
Notable services called by the UI: `frontend/dourememberfront/src/app/services/ground-truth-service.ts`, `frontend/dourememberfront/src/app/services/patientsService.ts`.

---

### 4) Activate Python venv and install dependencies

```bash
cd ../../backend/ai/ai_socket
python3 -m venv venv
source venv/bin/activate
# if repo has requirements.txt use it; otherwise install the basics used here:
pip install openai python-dotenv requests django djangorestframework
```

---

### 5) Run the AI socket server

From `backend/ai/ai_socket` (with the venv active):

```bash
python server.py
```

* The server will open a TCP socket (check `server.py` for HOST/PORT defaults).
* `server.py` reads `backend/ai/ai_socket/.env` for `api_key`.

---

### 6) Run the Django listener (REST → TCP forwarder)

From `backend/ai/ai_socket/userAnswerToGroundTestListener` (same venv):

```bash
python manage.py runserver 8000
# or to bind externally: python manage.py runserver 0.0.0.0:8000
```

* The Django view at `api/views.py` accepts user answers and posts them over TCP to the AI socket server. Ensure the HOST / PORT configured in `api/views.py` matches `server.py`.

---

## Expose frontend on port 80 (optional)

Angular dev server defaults to port **4200**. To make the app available at `http://localhost/` you can either:

**A. Bind `ng serve` to port 80** (may require `sudo` on Linux/macOS):

```bash
# from frontend/dourememberfront
sudo npm run start -- --port 80
# or sudo ng serve --port 80
```

**B. Forward host port 80 → 4200** (Linux example using `iptables`):

```bash
# requires sudo
sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 4200
```

**C. Use an HTTP reverse proxy (recommended for stable dev setups)** — example `nginx` snippet:

```nginx
server {
    listen 80;
    server_name localhost;

    location / {
        proxy_pass http://127.0.0.1:4200;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

After proxying or forwarding, open: `http://localhost/`.

---

## Configuration notes / environment variables

* **MySQL / Users DB**

  * Defaults are in `backend/users-backend/src/main/resources/application.properties` (check DB URL, username, password). Edit this file to point to your MySQL instance.

* **AI API key (.env)**

  * Create or edit `backend/ai/ai_socket/.env` and set:

    ```text
    api_key=sk-...your-openai-or-router-key...
    ```

* **Django listener → AI socket**

  * Open `backend/ai/ai_socket/userAnswerToGroundTestListener/api/views.py` and confirm the `HOST` / `PORT` constants (they must match what `server.py` is listening on).

* **Ports**

  * Spring Boot defaults: `8080` (changeable via `--server.port=` or `application.properties`).
  * Angular dev: `4200` (changeable with `--port`).
  * Django devserver default: `8000`.
  * AI socket server: check `server.py` for its default host/port.

---

## Troubleshooting

* **Port conflicts**: If a Spring Boot app fails to start because the port is in use, start it with `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=XXXX`.

* **DB connectivity**: Ensure MySQL is running and credentials in `application.properties` are correct. If you don't want MySQL, adjust `application.properties` to use an in-memory DB for local testing.

* **AI API key / OpenAI errors**: Confirm `backend/ai/ai_socket/.env` contains a valid `api_key`. Check network connectivity to the OpenAI/router endpoint from the machine running `server.py`.

* **Django → AI socket connection errors**: Verify `HOST`/`PORT` values in `userAnswerToGroundTestListener/api/views.py` match those in `server.py`. Check firewall / local network rules that might block TCP sockets.

* **CORS**: If the frontend cannot call the backends, check CORS configuration in the Spring Boot apps (controllers) and the Django listener.

* **Angular `ng serve` permission errors when binding port 80**: Use a reverse proxy (nginx) or port forwarding. Running `ng serve` as root is discouraged.

---

## Minimal dev checklist

* [ ] Configure `backend/users-backend/src/main/resources/application.properties` (DB URL / user / password).
* [ ] Create `backend/ai/ai_socket/.env` with `api_key`.
* [ ] Start Users + Groundtruth Spring Boot apps (or change ports to avoid conflicts).
* [ ] `npm install` and `npm run start` in `frontend/dourememberfront`.
* [ ] Activate venv and `python server.py` for AI socket, and `python manage.py runserver` for the Django listener.
* [ ] Confirm frontend calls to backends: check `frontend/dourememberfront/src/app/services/ground-truth-service.ts` and `patientsService.ts`.

---

## Useful code locations (quick reference)

* **Users backend**

  * `com.springboot.backend.salazar.usersbackend.users_backend.UsersBackendApplication`
  * `Controller`: `GroundTruthController` (`backend/users-backend/.../controllers/GroundTruthController.java`)
  * `Service impl`: `GroundTruthServiceImpl` (`.../services/GroundTruthServiceImpl.java`)
  * `Entities`: `backend/users-backend/src/main/java/.../entities/GroundTruthResponse.java`

* **Groundtruth backend**

  * `com.springboot.backend.douremember.groundtruth.groundtruth_backend.GroundtruthBackendApplication`
  * Controller: `backend/groundtruth-backend/src/main/java/.../GroundTruthController.java`

* **AI / Python**

  * AI socket server: `backend/ai/ai_socket/server.py`
  * Test client: `backend/ai/ai_socket/client.py`
  * Django listener: `backend/ai/ai_socket/userAnswerToGroundTestListener/manage.py`
  * Listener views: `backend/ai/ai_socket/userAnswerToGroundTestListener/api/views.py`

* **Frontend**

  * Angular entry: `frontend/dourememberfront/src/main.ts`
  * Routes: `frontend/dourememberfront/src/app/app.routes.ts`
  * Ground truth client: `frontend/dourememberfront/src/app/services/ground-truth-service.ts`
  * Patients service: `frontend/dourememberfront/src/app/services/patientsService.ts`

---
