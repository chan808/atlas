# M1.2 First Clarification Question Implementation Record

Status: implemented and verified
Completed: 2026-08-30
Contract: [`M1_FIRST_CLARIFICATION_SLICE.md`](M1_FIRST_CLARIFICATION_SLICE.md)
Independent review:
[`M1_FIRST_CLARIFICATION_IMPLEMENTATION_REVIEW.md`](M1_FIRST_CLARIFICATION_IMPLEMENTATION_REVIEW.md)

## Observable behavior

After a successful Brain Dump capture, the local user sees the exact stored raw
text and may explicitly request one clarification question. A successful start
shows one persisted question and the reason its answer matters, while keeping
the raw text visibly separate. Capture alone never starts clarification.

The page stops there. It has no answer control, later turn, Brief, Question Map,
authentication, live model, job, queue, or distributed infrastructure.

## Implemented boundary

- `Inquiry` now exposes an optimistic version and permits only the application
  transition from `CAPTURED` to `CLARIFYING`.
- Flyway V2 preserves populated V1 capture rows, backfills version `0`, and adds
  clarification start-request and first-turn tables.
- `ClarificationProposalPort` is product-owned. Its only adapter is an
  in-process deterministic fake with versioned, validated output and no
  provider SDK, network request, or model key.
- The start command and stable/current turn reads implement the reviewed HTTP
  contract and return non-echoing Problem Details on failure.
- The React flow owns a separate start idempotency key, prevents duplicate
  in-flight starts, and displays the question and reason with accessible
  headings.

## Data and transaction flow

```text
validate key and requested version
  -> replay a previously committed matching result when present
  -> read the captured Inquiry
  -> ask and validate the deterministic proposal outside a write transaction
  -> in one PostgreSQL transaction:
       claim (key, Inquiry, requested version)
       compare-and-set CAPTURED/version to CLARIFYING/version+1
       insert sequence-1 turn with the resulting version
  -> read and return the persisted turn
```

The database is the concurrency authority. A composite foreign key prevents a
turn from referencing a start request owned by another Inquiry. The raw Brain
Dump is never updated by this flow.

## Failure, duplication, and interruption outcomes

| Situation | Persisted result |
|---|---|
| Invalid or null proposal | Inquiry remains `CAPTURED`; no key or turn |
| Failure before commit | Claim, transition, and turn all roll back |
| Process stops before commit | Captured Inquiry and no turn |
| Process stops after commit but before response | One clarifying Inquiry and one turn; same-key retry returns it |
| Same key and canonical request | Original stable turn |
| Same key with another Inquiry/version | `409`; neither Inquiry changes |
| Different keys race on one Inquiry | One turn commits; the loser receives `409` |
| Web transport or `5xx` ambiguity | Raw text remains; manual retry reuses key and version |
| Web `409` | Start is not repeated; current turn is read once |
| Web `400`, `404`, or failed conflict recovery | Terminal message; raw text remains |

Problem responses and normal captured logs contain none of the fixture raw
text, question, reason, or idempotency key.

## Verification evidence

- Web verification: TypeScript typecheck, 20 Vitest tests, oxlint, and Vite
  production build pass.
- API verification: 33 JUnit tests pass against PostgreSQL Testcontainers,
  including same-key and different-key concurrency, transaction rollback,
  populated V1-to-V2 preservation, composite ownership rejection, HTTP error
  behavior, and Spring Modulith verification.
- Repository verification: `scripts/verify.ps1` passes without a model key or
  paid call.
- Independent implementation review first blocked on null proposal handling,
  unexpected HTTP failure redaction, and database ownership integrity. All
  three findings were fixed and the final API and web passes reported no
  remaining findings.

## Removal and rollback

Before retained external data exists, remove the M1.2 web action and server
code, then recreate the local database from V1. Do not edit an already-applied
V1 or V2 migration.

If data must be retained, first disable the web action and start endpoint. Keep
the additive version, start-request, and turn data until a separately reviewed
forward cleanup migration is allowed. Do not rewrite `CLARIFYING` history to
`CAPTURED` merely to make an older application accept it.

The next M1 slice must contract answering the first question before adding any
answer UI or second turn. The proposed selectable Question Map remains a later
M2 product decision.
