# AGENTS.md

Rules for Codex and other coding agents working in this repository.

## Project

Project Atlas helps a person turn an unstructured curiosity into an approved
learning brief, an answerable question, one concrete next action, and eventually
evidence of understanding.

Read `PRD.md`, `docs/TECH_SPEC.md`, `docs/FIRST_VERTICAL_SLICE.md`, and
`docs/AI_CHANGE_POLICY.md` before changing product or architecture code.

## Absolute product rules

1. Preserve the user's original words. Never replace the raw curiosity with an
   AI summary.
2. AI output is a proposal, not authority. AI cannot approve a learning brief,
   mark understanding as demonstrated, or issue a deterministic verdict.
3. The system asks only questions whose answers can change scope, direction, or
   evidence. Do not turn clarification into a long questionnaire.
4. `I do not know`, `both`, and `show me an example` are valid answers.
5. Do not show fake mastery percentages. A demonstrated claim must reference
   concrete evidence.
6. Prefer deterministic validation over LLM judgement whenever a property can
   be tested.
7. Do not add broad web crawling, autonomous tool use, remote code execution,
   or user-supplied container execution in the current milestone.

## Architecture rules

- Start as a Spring Boot modular monolith. Do not add a microservice, broker,
  cache, workflow engine, vector database, graph database, or Kubernetes without
  a measured need and an accepted ADR.
- The current code may contain only the modules needed by the active vertical
  slice. Do not scaffold future modules as empty abstractions.
- A module cannot access another module's repository, JPA entity, or tables.
- Product modules define AI ports. Provider SDKs and Spring AI types stay in AI
  adapter code and cannot leak into product domain code.
- Use Flyway for schema changes. Do not rely on Hibernate schema generation.
- Use UTC and inject `Clock` into domain logic instead of calling the system
  clock directly.
- External calls do not run inside database transactions.
- Create, approval, submission, and retry operations must become idempotent
  before they are exposed to external users.
- Secrets remain server-side and are never committed or sent to browser code.

## AI-assisted change rules

- One observable behavior per change.
- One new architectural risk axis per milestone.
- Before implementation, state the user behavior, invariants, failure cases,
  files affected, and verification plan.
- Core changes to transactions, concurrency, authentication, AI execution,
  migrations, isolation, or deployment require a written change brief,
  independent review, verification, and rollback notes. Owner teach-back is
  never a commit or merge gate.
- If two undocumented core changes accumulate, stop feature work and record the
  missing evidence before continuing.
- Use a separate review pass from the implementation pass.
- Do not weaken tests merely to make an AI-generated implementation pass.

## Coding conventions

- Java 21, strict null handling, constructor injection, no field injection.
- Keep domain behavior out of controllers and persistence entities.
- TypeScript stays in strict mode. Prefer semantic HTML and accessible controls.
- Comments explain non-obvious intent or constraints, not the code itself.
- No production `console.log` or swallowed exceptions.
- Do not refactor outside the requested scope.

## Verification

Run from the repository root:

```powershell
.\scripts\verify.ps1
```

CI and tests must not require a paid model call or a real model API key.

## Commits

Use English Conventional Commits: `<type>(optional-scope): <imperative summary>`.
Do not add a period to the summary.
