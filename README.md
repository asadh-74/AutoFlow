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

## Public demo behavior

Use fictional customer details only. Two **local demo** workflows (onboarding checklist and lead qualification) execute real backend logic and persist execution records without sending notifications. The two n8n integrations require privately configured production webhook URLs. A successful webhook response means accepted, not proof that every downstream action completed. Unknown workflow names are rejected.

Reports download current Java dashboard statistics through the Python service rather than fixed sample KPIs. CSV cleaning downloads the cleaned data. Unavailable APIs show an error/empty state rather than fabricated customer records. The custom-workflow dialog explains that new integrations must be configured in n8n.


## Complete recording workflow
1. Add a fictional customer in Customers; edit it and filter the list.
2. Open Automations, select that customer, and run **Demo customer onboarding**. Show the returned customer and checklist.
3. Run **Demo lead qualification** with high then low intent. Compare score and decision.
4. Open Execution History and use **View result** to retrieve a stored output from PostgreSQL.
5. Generate the daily report from current API counts and clean a CSV with duplicate rows. Show the visible summary and downloaded output.

Local demo workflows execute Java business logic without sending notifications. Production n8n workflows require their own configured webhooks; an unconfigured lead webhook returns 503. The public portfolio app is for fictional records. Free Render services may take time to wake.

## Client workflow upgrades
- Lead operations: `frontend/leads.html`. Validated fictional enquiries update the PostgreSQL CRM by normalized email. Submission IDs make retries idempotent; a changed payload with an existing ID returns 409. Customer update, staff inbox entry and execution audit commit in one transaction. Notifications are internal only.
- DocFlow: Python service `/docflow`. Upload an unlocked text PDF (2 MB, 20 pages), review label-extracted fields, correct arithmetic exceptions, approve and export CSV. Includes normal and incorrect-total sample PDFs. No OCR or universal-layout AI extraction is claimed; scanned/unsupported fields require manual review.
- DocFlow is browser-session isolated; SQLite demo records expire after 24 hours and reset on ephemeral hosting restarts. Do not upload private documents.
- Tests: Java `mvn test`; Python `pytest tests`. Use Java 21 in deployment.


## Agentic lead operations (new)

The Lead operations workspace now includes a bounded agent review before CRM capture:

1. Validate the name, company and enquiry.
2. Classify intent, urgency and priority; calculate a lead score.
3. Recommend a route or follow-up and prepare a reply draft.
4. Show a short trace and require a human to decide what happens next.
5. Continue through the existing idempotent CRM capture and internal inbox workflow.

The agent does **not** modify CRM data, send email, or call external tools. The demo works without credentials using explicitly labeled keyword rules. To enable Gemini for a private/local demo, set `GEMINI_API_KEY` and optionally `GEMINI_MODEL` in the Python service environment. Never commit the key or expose a paid key on an unauthenticated public service. Gemini output is schema-validated and still needs human review; AI output is not guaranteed to be correct.

- Endpoint: `POST /agent/lead-review`
- UI: `frontend/leads.html`
- Tests: `pytest tests` from `python-automation` (includes rules-mode, approval-gate and input validation checks)
- Current limits: lead review is advisory, not an autonomous CRM writer. External CRM/email integrations still require private n8n configuration.
