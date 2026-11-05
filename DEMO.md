# AutoFlow demo — 60–75 seconds

Use fictional inputs and the repository's local Python service. Leave provider keys unset; the fallback is rules-based and makes no external calls. This is a portfolio project, not a production CRM integration.

## Recording sequence

- **0–8 sec — Problem:** “AutoFlow helps a small team validate incoming leads, clean source data and see which workflow steps need review.”
- **8–22 sec — Lead triage:** Open the FastAPI `/docs` page and run `POST /agent/lead-review` with `examples/lead-review.json`. Show the result marked `provider: rules_demo`, the trace and `action_executed: false`.
- **22–38 sec — Data quality:** Send `examples/duplicate-customers.csv` to `/automation/clean-csv`. Show three input rows becoming two normalized rows and identify the duplicate.
- **38–54 sec — Document review:** In `/docflow`, load the provided incorrect-total sample PDF, show the detected issue, correct it, approve and export. Emphasize the human review step.
- **54–65 sec — Workflow tracking:** If the Java/PostgreSQL services are configured and the run succeeds locally, show the customer record and workflow status. Skip this segment if it is not running.
- **65–75 sec — Boundaries:** “n8n external actions need a configured endpoint and credentials. The demo uses deterministic rules, not a live AI agent; no customer email or CRM action is sent.”

## Expected result

The included CSV produces 3 input rows and 2 output rows. The lead review is high priority, awaits human approval and executes no external action.

## Screenshots

Capture the actual local API response, CSV before/after result and document review state using the sample files. Label captures “Local demo · fictional data · rules demo.” The dashboard artwork and README diagrams are illustrative. Do not present them as screenshots or claim an integration succeeded unless it is visible in the captured run.

## Limits

External n8n CRM/email actions require configuration and a separate test. Gemini mode requires a private valid key. The README records that Java tests still need a Java 21 environment; do not show Java/PostgreSQL as verified until that environment passes.
