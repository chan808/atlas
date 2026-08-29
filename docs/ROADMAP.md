# Project Atlas Roadmap

This roadmap is ordered by product risk, not by architectural ambition. Each
milestone introduces at most one unfamiliar technical axis and ends in an
observable user outcome.

## M0 — Repository and product contract

Outcome: a new contributor can understand the hypothesis and run a verified
Spring Boot, React, and PostgreSQL workspace.

- Product and technical specifications
- AI-assisted change policy and learning protocol
- Architecture decisions for product shape and modular monolith
- Java/React scaffolds, PostgreSQL Compose, tests, and CI
- No product domain implementation

Learning focus: distinguish product risk from technology risk.

Exit gate:

- all verification commands pass from a clean checkout;
- no paid model key is required;
- the owner can explain why M1 is smaller than the product MVP.

## M1 — Curiosity to approved Learning Brief

Outcome: a local user turns a Brain Dump into an approved Brief and one
30-minute action.

- `inquiry` product module only
- Flyway schema and PostgreSQL repositories
- Deterministic clarification/Brief/action proposal port
- Brain Dump, up to three clarification turns, Brief editing and approval
- Pause/resume and optimistic versioning
- Accessible web flow

New technical concept: aggregate state machine and transaction boundary.

Exit gate:

- founder completes six real inquiries over two weeks;
- 5-10 matching developers test the flow;
- at least 70% approve a useful Brief within ten minutes;
- product and technical acceptance tests pass.

M1 is delivered in small vertical slices. The first implementation slice,
M1.1, ends after a Brain Dump can be created idempotently, stored in PostgreSQL
without textual normalization, retrieved, and safely retried from the web UI.
That implementation is verified. Clarification, proposal ports, Briefs, and
actions remain later M1 slices, none of which has a slice-level engineering
contract yet. Completing M1.1 does not claim that the M1 product metrics have
been met.

## M1.5 — Live model adapter

Outcome: replace the deterministic proposal implementation without giving the
model authority over product state.

- Evaluation fixtures from real M1 inputs, redacted where necessary
- Versioned JSON schemas and prompt templates
- Bounded cost, timeout, retry, and privacy behavior
- Durable `AiRun` and job state
- Fake remains the default for CI

New technical concept: non-deterministic external work with durable state and
idempotent result application.

Exit gate:

- live proposals beat the reviewed fake/templates on usefulness without adding
  unsupported user facts;
- provider failure cannot corrupt an Inquiry;
- cost and latency are measured.

## M2 — Approved Question Map and first evidence

Outcome: an approved Brief becomes a small question map and one bounded learning
Expedition.

- Reviewed backend domain map for the supported concepts
- Proposed and approved Question Map revisions
- One recommended question with a visible reason
- Text/design Evidence and Reflection
- Resume view centered on context and next action

New technical concept: versioned graph-like relationships in relational data.

Exit gate:

- users understand why the first question was selected;
- evidence is attached before a question becomes `DEMONSTRATED`;
- relational queries remain understandable without a graph database.

## M3 — First deterministic Incident Lab

Outcome: a learner diagnoses and fixes a lost-response duplicate-payment
scenario and produces an Evidence Pack.

- Version-pinned failing system and fault scenario
- Hypothesis before hints
- Deterministic verifier and known-good solution
- Local execution only
- Evidence Pack containing code, tests, diagram, decision, trace, and reflection
- AI asks evidence-based review questions but cannot issue the verdict

New technical concept: deterministic failure injection and invariant testing.

Exit gate:

- failing baseline always fails and known-good solution always passes;
- 20-30 matching developers test the complete loop;
- completion and second-Expedition metrics meet the PRD threshold.

## M4 — Local Runner boundary

Outcome: trusted, version-pinned labs can be started, stopped, and cleaned up by
a dedicated local process.

- Separate Runner process and narrow command contract
- No Docker socket in the API process
- Allowlisted images and commands
- CPU, memory, time, disk, and network limits
- Reconciliation and cleanup after interruption
- Structured verifier report with trust level

New technical concept: process isolation and reconciliation.

Exit gate:

- threat model reviewed;
- forced process death leaves no unmanaged lab;
- the owner can explain every Runner privilege.

## M5 — Closed alpha

Outcome: external users can safely retain their own learning state.

- Authentication and workspace ownership
- Backup and restore drill
- Audit trail and deletion workflow
- Deployment, metrics, traces, logs, alerts, and runbooks
- User/model usage limits

New technical concept: multi-tenant security and operating a public service.

Exit gate:

- restore and rollback drills pass;
- no user content appears in default logs;
- authorization tests cover every aggregate access path.

## Scale and collaboration milestones

These are hypotheses, not scheduled work.

- Remote isolated Runner pool
- Real-time study cohort and presence
- Event replay and analytics
- Kafka, Kubernetes, or workflow engine only after the triggers in
  `docs/TECH_SPEC.md` are observed
- Additional backend incidents and later domain packs

## Stop rules

- Stop adding features if two core changes cannot be explained by the owner.
- Stop infrastructure work if a milestone produces no user-visible outcome.
- Revert an infrastructure choice when its measured benefit does not exceed its
  operating cost.
- Narrow to Incident Lab if users consistently skip clarification and maps.
- Redesign the action step if users approve Briefs but do not begin learning.
- Stop product work if the combined loop is not preferred to normal AI chat,
  search, and a local project after the PRD's validation window.
