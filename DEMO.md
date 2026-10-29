# AutoFlow demo — 90 seconds

Use fictional data only. For a no-credential demonstration, start the Python service using the README and leave GEMINI_API_KEY unset.

1. Open `/docs` on the Python service and run `POST /agent/lead-review` with `examples/lead-review.json`.
2. Show `provider: rules_demo`, the recommendation, the trace and `action_executed: false`. Explain that this mode is deterministic keyword routing, not an LLM.
3. POST `examples/duplicate-customers.csv` to `/automation/clean-csv`; show rowsBefore/rowsAfter and the downloaded cleaned CSV.
4. Open `/docflow`, load its provided incorrect-total sample PDF, show the validation issue, correct it, approve and export. Explain the review gate.
5. If the Java/PostgreSQL stack is configured, demonstrate lead capture and its duplicate-submission protection. Only describe this step as working after completing it on that deployment.
6. Close with the boundaries: external n8n CRM/email actions need private configuration; Gemini mode needs a valid key.

Expected cleanup result for the included CSV: 3 input rows, 2 output rows. Lead review should be high priority, await human approval and execute no external action.
