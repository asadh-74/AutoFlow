# Validation — 29 October 2026

- Python: 8 tests passed during the initial review using Python 3.12.
- Frontend syntax: see the current check evidence in the delivery report.
- Java: local execution blocked by Maven dependency resolution; installed runtime was Java 17 while project targets 21. This is an unverified layer, not a passing test.
- External PostgreSQL, n8n, CRM and email were not exercised against live client records.
- Gemini was not called. No API key from chat was used.

Reproduce: install the Python requirements plus pytest and httpx, then run `PYTHONPATH=. pytest -q tests` inside `python-automation`. Use Java 21 and `mvn test` in `java-backend`.
