# Project Atlas Technical Specification

Status: architecture baseline v0.1
Completed implementation milestone: M0 repository setup
Implemented engineering slice: M1.1 Brain Dump capture
Next engineering slice: M1.2 First Clarification Question contracted, not implemented

## 1. Design objective

The system must preserve a learner's unstructured intent, use AI only as a
non-authoritative proposal engine, and make every approved learning state
traceable to a user action and version.

The architecture must stay simple enough to inspect, operate, and recover.
Future distributed components are introduced only after their product or
operational need is measured.

## 2. Core flow

```text
raw curiosity
  -> structured clarification
  -> proposed Learning Brief
  -> explicit approval
  -> Question Map
  -> bounded Activity
  -> Evidence
  -> verification and reflection
```

AI may propose clarification, structure, feedback, and next steps. The
application owns state transitions. The user owns approval. A deterministic
verifier owns any verdict that can be computed objectively.

## 3. Architecture now versus later

### M0/M1 deployed architecture

```text
Browser
  -> React + TypeScript web app
  -> Spring Boot modular monolith
  -> PostgreSQL
```

The current code contains only the `inquiry` product module needed to capture
and retrieve a Brain Dump. M1.2 contracts the first clarification proposal
port, deterministic fake, persisted turn, and `CAPTURED -> CLARIFYING`
transition, but none is implemented yet. There is no Worker, Runner, message
broker, cache, vector store, or activity framework.

### Target product concepts, not initial modules

If validated milestones require them, the product may eventually contain these
bounded capabilities:

- `inquiry`: raw curiosity, clarification, Brief, Question Map, approval
- `learning`: Journey, focus question, cycle, reflection, next action
- `activity`: assignments, attempts, evidence, verification
- `incident`: versioned backend incident activities
- `ai`: provider adapters, prompt and schema versions, cost records
- `execution`: durable jobs, leases, retry, cancellation

Do not create empty packages or interfaces for these future concepts. A concept
becomes a module only when an implemented user behavior requires it.

## 4. Technology baseline

### API

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Bean Validation
- Spring Data JPA
- Flyway
- PostgreSQL 18
- Spring Boot Actuator
- Spring Modulith boundary verification
- JUnit 5 and Testcontainers
- Maven Wrapper

Java 21 is an intentional boring choice because it is already installed and is
supported by the selected Spring Boot line. A later Java upgrade is a separate
measured maintenance task, not part of product discovery.

### Web

- React 19
- TypeScript in strict mode
- Vite
- Vitest and Testing Library
- pnpm workspace

No router, server-state library, global state library, or component system is
added until multiple screens or repeated state make the need concrete.

### Local infrastructure

- Docker Compose
- One PostgreSQL container
- API and web processes run on the host

## 5. Code organization

The Java root package is `com.projectatlas`.

M1 begins with a feature package instead of global controller/service/repository
layers:

```text
com.projectatlas
  AtlasApplication
  inquiry
    domain
    application
    api
    infrastructure
```

Only add a subpackage when it contains more than one meaningful type. Small
features may stay flatter.

Module rules:

- A module does not use another module's JPA entity, repository, or table.
- Cross-module references use stable identifiers and public application APIs.
- ORM relationships never cross a module boundary.
- Provider SDK and Spring AI types cannot appear in product domain APIs.
- There is no `shared` dumping ground. Common code must be stable and minimal.
- `ApplicationModules.verify()` runs in the test suite.

## 6. M1 domain model

M1.1 implements only an `Inquiry` with an immutable raw value, `CAPTURED`
status, creation time, and creation idempotency key. The remaining types in this
section are M1 design constraints, not permission to scaffold them early. The
executable M1.1 contract is `docs/M1_CAPTURE_SLICE.md`.

The next executable contract is
`docs/M1_FIRST_CLARIFICATION_SLICE.md`. Until its implementation is complete,
the code and deployed behavior remain M1.1 only.

### Inquiry

An `Inquiry` preserves the raw input and coordinates clarification and Brief
approval.

Fields needed by the complete M1 design may eventually include:

- `InquiryId`
- `rawText`
- `status`
- `createdAt`
- `updatedAt`
- optimistic-lock `version`
- current proposed and approved Brief revision references

M1.1 does not add `updatedAt`, optimistic `version`, or Brief references because
it has no update operation. `rawText` is immutable after creation. A materially
different thought creates a new Inquiry; it does not overwrite history.

### ClarificationTurn

- stable turn identifier
- inquiry identifier
- sequence number
- proposed question
- reason the question can change the direction
- answer kind and optional free text
- created and answered times

The M1 flow asks at most three initial turns. Allowed answers include
`FREE_TEXT`, `DO_NOT_KNOW`, `BOTH`, and `SHOW_EXAMPLE`.

### LearningBriefRevision

- revision number
- motivation and context
- desired capability
- deliberately excluded scope
- constraints
- expected evidence
- unresolved questions and explicit assumptions
- proposal source and schema version
- proposed, approved, and superseded times

Editing a proposed Brief creates or updates an unapproved draft. Approval makes
the revision immutable. Later changes create a new revision.

### ExplorationAction

M1 returns one action that should fit in roughly 30 minutes.

- a single question to explore;
- why it is the next action;
- an observable completion result;
- a time box;
- an explicit stop condition.

It is not a full curriculum and does not imply mastery.

## 7. State machine

M1 Inquiry state:

```text
CAPTURED
  -> CLARIFYING
  -> BRIEF_PROPOSED
  -> BRIEF_APPROVED
  -> ACTION_PROPOSED
  -> READY
```

Additional terminal/supporting states:

- `PAUSED`: resumable without changing accepted history
- `ARCHIVED`: no longer active

Rules:

- Only application methods may perform transitions.
- Persistence callbacks and AI adapters cannot change state directly.
- Invalid transitions return a domain error and do not partially write data.
- Approval records the approved revision and actor in one transaction.
- A duplicate approval request returns the existing result when the
  idempotency key matches.

M2 adds proposed and approved Question Map states. M3 adds Activity Attempt
states. They are not modeled in M1 code.

## 8. Core invariants

- The original Brain Dump is always retrievable exactly as submitted.
- A clarification turn records why it was asked.
- The initial M1 sequence cannot exceed three answered clarification questions.
- `DO_NOT_KNOW` is accepted without forcing free text.
- AI output cannot approve, archive, or mark an Inquiry ready.
- An approved Brief revision is immutable.
- An action can only reference an approved Brief revision.
- One Inquiry has at most one active proposed action in M1.
- Repeating an approval or create request with the same idempotency key cannot
  create duplicate business results.
- No user text or model prompt is written to normal application logs.

## 9. AI boundary

Product code defines use-case-specific ports, not a generic agent interface.

M1 may define:

```text
ClarificationProposalPort
LearningBriefProposalPort
ExplorationActionProposalPort
```

The deterministic fake must be implemented before a live provider adapter. CI,
tests, and a local demo can complete without a model key.

Structured outputs have versioned schemas. Validation includes:

- unknown fields rejected;
- string and collection limits;
- no more than the allowed question count;
- allowed answer kinds only;
- explicit assumptions rather than invented user facts;
- no unsupported activity type;
- references point to existing records.

A schema failure never mutates product state. At most one bounded repair attempt
may be added with the live adapter; repeated failure becomes a visible retryable
error.

When live model calls arrive, record an `AiRun` with provider, model, prompt
version, output schema version, input digest, status, timing, attempts, token
usage, estimated cost, and error code. Do not claim that this metadata makes a
non-deterministic model response reproducible.

External model calls do not execute in a database transaction. The first live
adapter milestone introduces durable jobs before exposing the call to multiple
users.

## 10. Persistence and migrations

- PostgreSQL is the only data store.
- Flyway is the only schema change mechanism.
- Hibernate uses `ddl-auto=validate`.
- Migrations are forward-compatible with the currently deployed application
  during public operation.
- Destructive migrations require an explicit migration and rollback plan.
- JSONB is reserved for versioned AI payloads whose shape is validated at the
  boundary; core relational state stays relational.
- Question Maps use relational node and edge tables when M2 arrives. A graph
  database is not justified.

All domain time uses `Instant` in UTC and an injected `Clock`. IDs are generated
in the application and are not treated as authorization.

## 11. HTTP conventions

- APIs are rooted at `/api`.
- Errors use Problem Details (`application/problem+json`).
- Validation errors identify fields without echoing sensitive raw content.
- Create, approval, submission, and retry endpoints accept an
  `Idempotency-Key` before public exposure.
- Aggregate mutations use optimistic versions and return conflict on stale
  updates.
- The web development server proxies `/api`; the backend does not enable a
  wildcard CORS policy for convenience.

The implemented capture API contract is in `docs/M1_CAPTURE_SLICE.md`; the
contracted first-question API is in
`docs/M1_FIRST_CLARIFICATION_SLICE.md`. The wider M1 product contract remains in
`docs/FIRST_VERTICAL_SLICE.md` and may change before each later slice is
implemented. An OpenAPI generator is added only when another client or contract
publication creates a concrete need; the current local web/API boundary is
covered by executable HTTP tests.

## 12. Security and privacy baseline

- M0/M1 is local-only until authentication and ownership checks are implemented.
- Model and database credentials remain in server-side environment variables or
  a later secret store.
- No model key is placed in `VITE_*` variables.
- User input and AI output are untrusted data.
- The web app does not render AI HTML with `dangerouslySetInnerHTML`.
- The model receives no URL-fetch, database-mutation, shell, or code-execution
  tool in the current milestones.
- Arbitrary URL fetching and file upload are excluded to avoid premature SSRF
  and content-processing attack surfaces.
- Daily model cost, concurrency, and token limits are required before external
  users receive live AI access.

Remote lab execution has a separate future threat model. The API process will
never mount a Docker socket or run user-provided commands.

## 13. Testing strategy

### API

- Domain unit tests for state transitions and invariants
- PostgreSQL Testcontainers tests for repositories, Flyway, locking, and
  transaction behavior
- Spring Modulith verification
- API tests for validation, idempotency, invalid state transitions, and later
  workspace isolation
- AI contract tests against fixtures and fakes, never exact prose equality

### Web

- M1.1 component tests for blank and oversized input, exact request values,
  retry-key reuse, duplicate-submit prevention, and preserving input on failure
- Later M1 component tests for one-question display, alternative answers, Brief
  editing, and approval when those behaviors are implemented
- A small number of automated end-to-end tests when the first multi-step M1
  path makes the browser/API boundary regression-prone
- Accessibility assertions on the primary flow

### AI evaluation

A later `docs/evals` set measures schema validity, faithfulness to the user's
words, usefulness of clarification, unsupported assumptions, action size, and
evidence alignment. Paid provider smoke tests are manual or budget-limited and
never gate normal CI.

## 14. Evolution triggers

| Addition | Required evidence |
|---|---|
| Live AI adapter | Fake-backed flow is useful and approved schemas are stable |
| Durable job queue | External calls are slow/failing or need cancellation/retry |
| Independent Worker | Jobs affect API latency or need independent scaling |
| SSE | Polling creates measured user experience problems |
| Object storage | Real file evidence or large artifacts enter scope |
| RAG/pgvector | Approved sources and grounded retrieval prove user value |
| Incident Runner | The local incident activity is valuable enough to automate |
| Redis | A measured cross-instance ephemeral or caching need exists |
| Kafka | Multiple independent replay consumers and PostgreSQL limits are observed |
| Temporal | Long human waits, timers, and compensation overwhelm the explicit state machine |
| Kubernetes | Multiple isolated runners require scheduling and reconciliation |
| Microservice | Security boundary, independent scale, or deployment lifecycle demands it |
| Graph database | Real map queries are not maintainable with relational recursion |

Each addition requires a small experiment, an ADR, a removal path, and before/
after measurements. A scale lab may explore a technology without promoting it
into the product runtime.

## 15. Operational completion rule

A milestone is complete only when repository evidence records:

- the user behavior that changed;
- the request and data flow;
- the transaction boundary;
- the result of timeout, duplication, and process death;
- the test proving each invariant;
- the rollback or removal path.

Tests, documentation, review, and rollback evidence determine completion. The
owner's current understanding or availability never blocks commit or merge.
