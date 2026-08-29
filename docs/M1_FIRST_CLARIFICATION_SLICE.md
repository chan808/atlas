# M1.2 First Clarification Question Contract

Status: contracted; implementation not started
Product milestone: M1 curiosity to approved Learning Brief
Depends on: completed M1.1 Brain Dump capture
Independent review: accepted in
[`M1_FIRST_CLARIFICATION_SLICE_REVIEW.md`](M1_FIRST_CLARIFICATION_SLICE_REVIEW.md)

## Product-direction check

M1.2 keeps the product hypothesis narrow: clarification is useful only if one
answer can change the learning direction or the evidence worth producing. It
does not treat conversation itself as progress and does not add a generic chat
surface.

The smallest observable behavior after capture is therefore:

> A local user explicitly starts clarification for a captured Brain Dump and
> sees one persisted question together with the reason its answer matters,
> while the original words remain visible and unchanged.

This slice tests whether the first question feels consequential and safe to
continue. It does not test whether the user can complete clarification or
approve a useful Learning Brief. If dogfooding repeatedly finds the first
question redundant or unable to change direction, revise or remove the
clarification approach before adding more turns.

M1.2 dogfooding and UI positioning support only the initial backend-systems and
reliability audience described in the PRD. The capture boundary remains
content-agnostic and M1.2 does not add a domain classifier, but neither the copy
nor the test evidence may imply that Atlas supports arbitrary subjects.

## Scope

M1.2 includes only:

- an explicit web action from the captured state to request the first question;
- one persisted `ClarificationTurn` with sequence `1`, question, reason, source
  metadata, creation time, and the Inquiry version produced by the transition;
- the `CAPTURED -> CLARIFYING` Inquiry transition with optimistic versioning;
- a product-defined clarification proposal port and one in-process,
  deterministic fake implementation;
- retry-safe start and retrieve HTTP behavior;
- a forward Flyway migration and automated API, persistence, and web tests.

M1.2 explicitly excludes:

- submitting any answer, creating a second or third turn, or completing
  clarification;
- `DO_NOT_KNOW`, `BOTH`, and `SHOW_EXAMPLE` controls, which become executable
  only with the answer slice;
- Learning Briefs, approval, Exploration Actions, and Question Maps;
- a live model, provider SDK, prompt execution, `AiRun`, repair attempt, job,
  queue, streaming, or network call;
- authentication, workspaces, resume lists, routing, or multi-user behavior;
- Kafka, a cache, a vector store, a workflow engine, or a new product module.

## First-question contract

The first question narrows the kind of outcome the user wants before Atlas
asks about technologies or prerequisites. The reviewed deterministic fake uses
one versioned template whose intent is equivalent to:

```text
Question
For this curiosity, what would be more useful first: being able to explain the
underlying idea, or checking it directly with a small example?

Reason
The answer changes whether the next clarification should narrow a conceptual
boundary or a build/diagnosis situation, and what later evidence would be
useful.
```

Displayed Korean copy may express the same intent. The proposal is not labelled
as AI intelligence. It must not claim facts about the user, copy the Brain Dump
into a generated summary, prescribe a technology, or imply that the user has
approved a direction.

The product-defined proposal contains:

- `question` and `reason`, each with at least one non-whitespace code point and
  at most 500 Unicode code points;
- no U+0000 or unpaired UTF-16 surrogate in either text field, with no silent
  normalization or replacement;
- a stable proposal source identifier and output schema version, each 1-64
  characters from ASCII letters, digits, `.`, `_`, and `-`.

Unknown or invalid proposal fields are rejected at the application boundary.
Semantic usefulness is reviewed with fixtures and dogfooding; the application
does not pretend that a deterministic validator can prove a question is good.

## Domain and state contract

Starting clarification creates one stable turn and changes the Inquiry in one
database transaction:

```text
Inquiry(CAPTURED, version 0) + no turns
  -> Inquiry(CLARIFYING, version 1) + ClarificationTurn(sequence 1)
```

The exact starting version need not always be zero if a future compatible
change adds unrelated mutations, but the request version must match the stored
version and the stored state must be `CAPTURED`.

Invariants:

- the stored Brain Dump is neither updated nor passed through normalization;
- only the application use case changes Inquiry state;
- the proposal port cannot persist a turn or change Inquiry state;
- one Inquiry has at most one sequence-1 turn and at most one unanswered active
  turn in this slice;
- every stored question has a non-empty stored reason;
- the first sequence is exactly `1`; no later sequence can be created by this
  slice;
- creation of the turn and transition to `CLARIFYING` commit together;
- the committed turn records the resulting Inquiry version so a retry returns
  the same resource representation;
- no raw Brain Dump, question, reason, or idempotency key appears in normal
  application logs or Problem Details.

## V2 migration outcome

M1.2 uses a new forward migration; V1 is never edited. V2 must:

- replace the named Inquiry status check so only `CAPTURED` and `CLARIFYING`
  are accepted;
- add a non-negative `version` column, backfill every V1 row to `0`, and leave
  the column non-null with a default suitable for newly captured Inquiries;
- create a clarification-start request table whose globally unique visible-
  ASCII key binds one Inquiry ID and one non-negative requested version;
- create a clarification-turn table owned by `inquiries`, with stable UUID,
  sequence, question, reason, proposal source, schema version, created time,
  resulting Inquiry version, and a unique reference to its start request;
- enforce sequence `1` for this slice, uniqueness of `(inquiry_id, sequence)`,
  one turn per start request, non-negative/resulting version constraints, the
  text and metadata limits in this contract, and referential integrity without
  cross-module ownership.

The migration does not rewrite `raw_text`, `created_at`, Inquiry IDs, or M1.1
creation idempotency keys. The upgrade evidence starts from a populated schema
at Flyway version 1, records those exact values, applies V2, and compares them
afterward; an empty-schema migration alone is not sufficient.

## Proposal and transaction flow

The orchestration boundary is explicit even though the first provider is an
in-process deterministic fake:

```text
validate request and check an existing idempotent result
  -> read the captured Inquiry snapshot
  -> ask ClarificationProposalPort outside a write transaction
  -> validate the proposed question and reason
  -> in one transaction claim the idempotency key in PostgreSQL,
     compare version and state, insert turn, change Inquiry to CLARIFYING,
     and increment version
  -> return the persisted turn
```

The pre-transaction idempotency lookup is only an optimization. Inside the
transaction, `INSERT ... ON CONFLICT DO NOTHING` claims the key in a dedicated
clarification-start request record. The key, canonical request, turn, and
Inquiry compare-and-set are the database authority and commit together. If a
concurrent transaction already claimed the key, the loser reads the committed
canonical request and turn: it returns that turn for a match and returns `409`
for a mismatch. A rollback removes the claim, so a failed attempt does not
consume the key.

This does not introduce durable external work. A future live provider cannot be
dropped into this synchronous flow; M1.5 must add its separately reviewed job,
timeout, cost, and result-application contract.

## HTTP contract

M1.2 adds `version` to the successful create and retrieve Inquiry
representations. Existing M1.1 fields and semantics remain unchanged.

```json
{
  "id": "4ee1572a-ecf1-4ce4-a661-e58094f9255e",
  "rawText": "the decoded input, unchanged",
  "status": "CAPTURED",
  "version": 0,
  "createdAt": "2026-08-28T00:00:00Z"
}
```

Retrieval after a successful start returns `status: "CLARIFYING"` and
`version: 1` while preserving the other Inquiry fields.

### Start clarification

```http
POST /api/inquiries/{inquiryId}/clarification-turns
Idempotency-Key: <client-generated-key>
Content-Type: application/json

{ "inquiryVersion": 0 }
```

The key follows the M1.1 visible-ASCII, 1-128 character rule. A successful
start and a successful same-request replay both return `201 Created`, with
`Location` pointing to the retrieve-turn endpoint and this persisted body:

```json
{
  "id": "3d2c2bd1-53d6-40c7-831e-7185d10eced2",
  "inquiryId": "4ee1572a-ecf1-4ce4-a661-e58094f9255e",
  "sequence": 1,
  "question": "...",
  "reason": "...",
  "inquiryVersion": 1,
  "createdAt": "2026-08-28T00:01:00Z"
}
```

`proposalSource` and schema version are stored for traceability but are not
needed in the user-facing response.

### Retrieve a stable turn resource

```http
GET /api/inquiries/{inquiryId}/clarification-turns/{turnId}
```

The turn belonging to the Inquiry returns `200 OK` with the same persisted
body. An unknown Inquiry, unknown turn, or turn owned by another Inquiry returns
`404` without revealing whether that turn exists elsewhere. The start
response's `Location` points to this stable resource, so later turns cannot
change what the URL identifies.

### Retrieve the current turn

```http
GET /api/inquiries/{inquiryId}/clarification-turns/current
```

The current turn returns `200 OK` with the same body. An unknown Inquiry or an
Inquiry without a current turn returns `404`. Malformed UUIDs return `400`.
This singleton discovery route is the only M1.2 resume aid; a resume list and
browser routing remain out of scope.

### Error behavior

Errors use `application/problem+json` and contain no user or proposal text.

- `400 Bad Request`: malformed JSON or UUID, missing/invalid key, negative or
  missing version;
- `404 Not Found`: the target Inquiry, stable child turn, or current turn is
  absent or does not match the requested ownership boundary;
- `409 Conflict`: stale version, Inquiry not in `CAPTURED`, or an idempotency
  key reused for a different Inquiry or request version;
- `500 Internal Server Error`: invalid fake output or an unexpected failure,
  with no partial mutation.

## Idempotency, concurrency, and failure contract

The start key identifies the canonical request `(inquiryId, inquiryVersion)`.
The database, not an in-memory check, is the concurrency authority.

- same key and same canonical request: return the original stored turn, even if
  the first response was lost;
- same key and a different canonical request: return `409` and change neither
  Inquiry;
- different keys racing for one captured Inquiry: exactly one request may
  create sequence 1 and advance the version; the other returns `409`;
- a different key after clarification has started returns `409` and cannot
  create another turn;
- proposal or validation failure before the transaction leaves the Inquiry
  `CAPTURED` and stores no turn or key result;
- database failure rolls back the turn, status, version, and idempotency result
  together;
- a failed attempt does not consume the key; the same canonical request may
  succeed with that key after the retryable cause is removed;
- process death before commit leaves the Inquiry captured with no turn;
- process death after commit but before response leaves one `CLARIFYING`
  Inquiry and one turn; retry with the same key returns it.

No external call or paid model call occurs anywhere in this slice.

## Web behavior

- The captured success card continues to show the server-returned raw value
  exactly, using whitespace-preserving presentation.
- A labelled button explicitly starts clarification; capture success alone does
  not imply that clarification started.
- While the start request is in flight, another start is disabled.
- Failure is announced with `role="alert"`; the captured text remains visible.
- An ambiguous transport failure or `5xx` retry reuses the same start key and
  request version.
- A `400` or `404` is terminal for that request and is not retried
  automatically. On `409`, the web does not repeat the start command; it reads
  the current turn once and renders the committed winner when present,
  otherwise it shows a terminal conflict while preserving the captured text.
- Success shows exactly one question and its reason under distinct headings,
  with the original Brain Dump still distinguishable from the proposal.
- The page does not render an answer form, Brief preview, progress score, or
  fake continuation. It states that answering is outside the current slice.

## Proposed implementation units

Implementation remains separate from this contract change.

1. **Domain and persistence.** Add the first-turn value rules, Inquiry
   transition/version, forward migration, repository compare-and-set behavior,
   atomic clarification-start request claim, and transactional application
   method. Review migration, concurrency, and rollback evidence independently
   before continuing.
2. **Proposal boundary and HTTP.** Add the use-case-specific proposal port,
   deterministic fake, orchestration outside the transaction, start/retrieve
   endpoints, Problem Details mappings, and API representations. Do not add a
   generic agent abstraction or provider dependency.
3. **Web first-question flow.** Add the explicit start action, retry-key reuse,
   accessible pending/error/success states, question/reason presentation, and
   the backward-compatible Inquiry `version` field.
4. **Closeout evidence.** Run the full verification script, perform a fresh
   independent review of the implementation diff, record the transaction and
   failure outcomes, and update milestone status without requiring owner
   teach-back.

Each unit supports the same one observable behavior and can be reverted without
starting answer or Brief work.

## Test and completion plan

The implementation is complete only when automated tests demonstrate:

- only `CAPTURED` can transition to `CLARIFYING`, with one version increment;
- question, reason, sequence, source metadata, created time, and resulting
  version survive a PostgreSQL round trip;
- a populated V1 database upgrades through V2 without changing an Inquiry's
  ID, exact raw text, status, creation timestamp, or creation idempotency key,
  and backfills version `0`;
- exact raw text containing whitespace, CRLF/LF, Korean, emoji, and decomposed
  combining characters is equal as the decoded Java string before and after
  starting clarification;
- turn insert and Inquiry transition roll back together on failure;
- same-key sequential and concurrent replay returns one stable turn;
- proposal or database failure does not consume the key, and the same canonical
  request can later succeed with it;
- different-key concurrency creates one turn and returns one conflict;
- stale version, wrong state, invalid proposal, and key-reuse conflict do not
  mutate the aggregate;
- start/stable-turn/current-turn retrieval status codes, stable `Location`,
  child ownership, malformed input, and non-echoing Problem Details follow this
  contract;
- the deterministic fake satisfies the versioned proposal boundary without a
  network or model key;
- blank, oversized, NUL-containing, unpaired-surrogate, and invalid-metadata
  proposals are rejected without a state, turn, or key mutation;
- the web preserves and distinguishes raw text, disables duplicate start,
  reuses the key only after ambiguous/`5xx` failure, recovers the current turn
  after a winning `409`, treats terminal errors as terminal, announces errors,
  and renders one question plus one reason by keyboard-accessible controls;
- captured log output for conflict, invalid-proposal, transaction-failure, and
  unexpected-failure paths contains none of the fixture raw text, question,
  reason, or idempotency key;
- M1.1 exact-text and create-idempotency behavioral assertions remain intact;
  fixtures may add the backward-compatible `version` field;
- Flyway applies V1 and V2 to an empty database and performs the populated V1
  upgrade described above;
- Modulith verification and `./scripts/verify.ps1` pass without a paid call.

Exact prose equality is not a domain invariant. Tests assert the fake fixture
at its adapter boundary and assert meaning-bearing fields, limits, and source
metadata at product boundaries.

## Removal and rollback

Before external users exist, remove the web start flow and M1.2 code, then
recreate the local database without the forward migration. Never edit V1 after
M1.2 has been applied.

If retained data exists, first disable the start endpoint and web action. Keep
the additive version column, clarification-start request table, and turn table
until a separate data-retention decision permits a forward cleanup migration.
Do not silently convert `CLARIFYING` rows back to `CAPTURED`, because that would
misrepresent persisted history.
