# AutoFlow — Intelligent Business Automation Platform

AutoFlow is a portfolio-grade full-stack business automation system that combines **Java, Python, SQL, n8n, HTML/CSS/JavaScript, Docker and Render** in one coherent project.

## Stack

- **Frontend:** HTML5, CSS3, JavaScript, Chart.js
- **Core API:** Java 21 + Spring Boot
- **Automation service:** Python 3.12 + FastAPI
- **Workflow orchestration:** n8n
- **Database:** PostgreSQL
- **Deployment:** Docker + Render Blueprint
- **CI:** GitHub Actions

## Features

- Responsive SaaS dashboard
- Dark/light UI
- Command palette (Ctrl/Cmd + K)
- KPI and analytics charts
- Customer CRUD
- PostgreSQL persistence
- n8n webhook triggering
- Workflow execution tracking
- FastAPI CSV cleaning
- FastAPI operational reports
- Service health display
- Render deployment blueprint
- Dockerized Java and Python services
- Importable n8n workflow template

## Architecture

```text
Browser Dashboard
      |
      v
Java Spring Boot API ------> PostgreSQL
      |
      +--------------------> n8n
                                |
                        external actions
                                |
      +--------------------> Python FastAPI
                        reports / CSV helpers
```

## Local run

### PostgreSQL
Create a local database named `autoflow`.

### Java API

```bash
cd java-backend
mvn spring-boot:run
```

Environment variables:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=autoflow
DB_USER=postgres
DB_PASSWORD=postgres
N8N_WEBHOOK_URL=https://YOUR-N8N-HOST/webhook/autoflow
N8N_WEBHOOK_SECRET=replace-me
```

### Python service

```bash
cd python-automation
python -m venv .venv
# Windows: .venv\Scripts\activate
# Linux/macOS: source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
```

### Frontend

```bash
cd frontend
python -m http.server 5173
```

Open `http://localhost:5173`.

## n8n

Import:

```text
n8n-workflows/customer-onboarding.json
```

Activate the imported workflow, copy its **Production Webhook URL**, and set it as `N8N_WEBHOOK_URL` in the Java service.

The dashboard sends workflow events to:

```http
POST /api/automations/trigger
```

Spring Boot forwards the event to n8n and stores the execution result in PostgreSQL.

## Render

The root `render.yaml` defines:

- `autoflow-java-api`
- `autoflow-python-service`
- `autoflow-frontend`
- `autoflow-db`

To deploy:

1. Open Render.
2. Select **New → Blueprint**.
3. Connect this GitHub repository.
4. Approve the resources from `render.yaml`.
5. Enter `N8N_WEBHOOK_URL` and `N8N_WEBHOOK_SECRET` when prompted.
6. Deploy.
7. Verify:
   - Java: `/api/health`
   - Python: `/health`
   - Frontend: root URL

> If Render assigns different service hostnames from the expected names, update `frontend/config.js` with the generated Java and Python service URLs.

## Main API routes

### Java

- `GET /api/health`
- `GET /api/dashboard`
- `GET /api/customers`
- `POST /api/customers`
- `PUT /api/customers/{id}`
- `DELETE /api/customers/{id}`
- `GET /api/executions`
- `POST /api/automations/trigger`

### Python

- `GET /health`
- `POST /reports/daily`
- `POST /automation/clean-csv`
- `POST /automation/prepare-message`

## Portfolio description

**AutoFlow — Full-Stack Business Process Automation Platform**

Designed a multi-service automation platform using Java Spring Boot, Python FastAPI, PostgreSQL, n8n, HTML/CSS/JavaScript, REST APIs and Docker. Implemented workflow triggering, customer management, execution tracking, automation utilities, reporting, responsive dashboards and Render-ready infrastructure.

## Security

Never commit:

- n8n webhook secrets
- database passwords
- SMTP credentials
- API keys
- access tokens

Use Render environment variables for production secrets.
