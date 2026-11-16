# Development Rules

The authoritative requirements are `docs/assignment(2026).pdf` and `docs/中期设计文档.md`. The latter defines the concrete architecture, states, permissions, database constraints, and API contracts.

## Technology Baseline

- Backend: Java 21, Spring Boot 3.x, Maven, Spring Security, MyBatis, MySQL 8, Flyway.
- Frontend: Vue 3, TypeScript, Vite, Element Plus, Vue Router, Axios.
- Deployment: Docker Compose with persistent database and attachment volumes.

## Engineering Rules

- Preserve existing Git history and user changes. Do not reset, rebase, amend, force-push, or bulk-delete untracked files.
- Keep controllers at the HTTP boundary. Business transactions belong in application services; controllers must not call mappers directly or write another module's tables.
- Enforce permissions on the server for lists, details, workflow actions, attachments, notifications, statistics, and administration. Hiding a button is not authorization.
- Use integer minor currency units or an explicitly justified exact decimal type. Never use binary floating point for business amounts or workflow thresholds.
- Workflow state, tasks, logs, notifications, and idempotency results that describe one action must be committed in the transaction defined by the design.
- Every database change requires a versioned migration, relevant constraints and indexes, and an integration test. Document rollback or forward-fix handling.
- Validate upload type, size, and count. Authorize preview and download through the owning business resource; never expose storage paths.
- Add focused tests for each change. Workflow changes must cover self-approval, non-assignee access, repeated requests, and concurrent state changes where applicable.
- Do not commit `.env`, secrets, tokens, real receipts, uploaded files, database volumes, dependency directories, or build output.
- Do not implement deferred features such as `HARD_LIMIT`, OCR, cross-application duplicate receipt detection, or a standalone mobile client without explicit approval.
- Keep each commit buildable and scoped to one verifiable change. Update migrations, tests, and documentation together when their contract changes.
