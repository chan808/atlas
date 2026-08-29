# M1.1 Brain Dump Capture Contract

Status: implementation verified; merge pending owner teach-back
Product milestone: M1 curiosity to approved Learning Brief

## User outcome

A local user can write an unfinished technical curiosity, save it without first
turning it into a polished question, and read back exactly the same decoded
Unicode string. A failed or repeated submission does not erase the draft or
create duplicate state.

M1.1 proves one prerequisite of Atlas: preservation before interpretation. It
does not yet test whether Atlas can clarify the thought or produce a useful
Learning Brief.

## Scope

M1.1 includes only:

- one `inquiry` product module;
- PostgreSQL storage created by one Flyway migration;
- create and retrieve HTTP endpoints;
- creation idempotency, including concurrent retries;
- a web capture form with explicit success, submitting, and failure states;
- tests for preservation, validation, retry, transaction, and web behavior.

M1.1 explicitly excludes:

- `ClarificationTurn`, proposal ports, and deterministic or live AI adapters;
- Learning Briefs, approval, Question Maps, and Exploration Actions;
- update, delete, archive, resume-list, authentication, or multiple users;
- a router, client state library, broker, cache, vector store, or deployment
  platform.

## Raw-text invariant

The preserved value is the Java string produced after JSON decoding, not the
original HTTP bytes or JSON escape spelling.

- Store and return the value without `trim`, `strip`, newline conversion, or
  Unicode normalization.
- Preserve leading and trailing whitespace, repeated spaces, tabs, CRLF and LF,
  Korean text, emoji, and combining characters.
- Validate without replacing the stored value.
- Require at least one non-whitespace code point.
- Allow at most 10,000 Unicode code points, not 10,000 UTF-16 code units.
- Reject U+0000 because PostgreSQL text cannot store it.
- Reject an unpaired UTF-16 surrogate rather than silently replacing it.

## HTTP contract

### Create

```http
POST /api/inquiries
Idempotency-Key: <client-generated-key>
Content-Type: application/json

{ "rawText": "..." }
```

The key is required and contains 1-128 visible ASCII characters (`!` through
`~`). It is operation metadata and is never returned or echoed in an error.

Successful creation and a successful replay both return:

```http
201 Created
Location: /api/inquiries/{id}
```

```json
{
  "id": "4ee1572a-ecf1-4ce4-a661-e58094f9255e",
  "rawText": "the decoded input, unchanged",
  "status": "CAPTURED",
  "createdAt": "2026-08-28T00:00:00Z"
}
```

### Retrieve

```http
GET /api/inquiries/{id}
```

An existing Inquiry returns `200 OK` with the same representation. An unknown
valid UUID returns `404`. A malformed UUID returns `400`.

### Errors

Errors use `application/problem+json` and do not include the rejected raw text,
idempotency key, stack trace, or persistence details.

- `400 Bad Request`: malformed JSON or UUID, missing or invalid key, or invalid
  Brain Dump;
- `404 Not Found`: no Inquiry has the requested ID;
- `409 Conflict`: an existing key is reused with a different decoded raw value;
- `500 Internal Server Error`: an unexpected failure with no partial write.

## Idempotency and transaction contract

The creation key is stored on the Inquiry row. A database unique constraint is
the concurrency authority; the application does not use a check-then-insert
race.

- Same key and same raw text: return the original row, ID, timestamp, body, and
  `Location`; the table still contains one row.
- Same key and different raw text: return `409`; the original row is unchanged.
- Different keys and the same raw text: create distinct Inquiries because the
  user may intentionally record the thought twice.
- Concurrent requests with one key: at most one row is committed.
- Validation failure does not consume the key.

The use case runs in one database transaction: attempt an insert with
`ON CONFLICT DO NOTHING`, then read the authoritative row by key and compare the
exact raw value. The response is built from that persisted row so an initial
response and a replay cannot differ because of database timestamp precision.

If the process dies before commit, no row remains. If it dies after commit but
before the response arrives, retrying the same request returns the committed
row. No external call occurs in the transaction.

## Web behavior

- A labelled textarea accepts the unpolished thought and remains editable.
- Starter prompts fill the textarea but never submit automatically.
- Blank or oversized content is stopped before the request and remains visible.
- While a request is in flight, another submit is disabled.
- A network or server error is announced with `role="alert"`; the textarea is
  not cleared.
- Retrying unchanged text reuses the same key, covering a lost response.
- Editing after a failed attempt creates a new key on the next submit.
- Success shows the server-returned raw value and `CAPTURED` state. It does not
  pretend that clarification has begun.

## Completion evidence

The slice is complete only when automated tests demonstrate:

- exact round trips for whitespace, CRLF/LF, tabs, Korean, emoji, and decomposed
  combining characters;
- one-code-point and 10,000-code-point boundaries, plus blank, NUL, unpaired
  surrogate, and 10,001-code-point rejection;
- sequential and concurrent same-key replay, conflicting payload behavior, and
  distinct-key behavior;
- `201`, `Location`, `200`, `400`, `404`, and `409` HTTP behavior;
- errors do not echo raw text or keys;
- failed web requests preserve both the draft and retry key;
- the `inquiry` module is discovered and Modulith boundaries remain valid;
- Flyway applies to an empty PostgreSQL database and the full repository
  verification passes without a paid model call.

The implementation evidence is executable in the repository: API tests cover
domain validation, PostgreSQL retry/concurrency, HTTP behavior, privacy, and
Modulith discovery; web tests cover form behavior, exact request values,
retry-key reuse, and duplicate-submit prevention. Local browser QA also checks
the responsive form and an API-down failure followed by a successful retry.

## Removal and rollback

Before external users exist, the feature can be removed by reverting the web,
API, and V1 migration together and recreating the local development database.
Once a deployed database contains user data, never edit V1; disable the create
route and add a forward migration or restore from backup according to an
explicit data-retention decision.

## Owner teach-back

Before merge, the owner must answer and explain:

1. why exact preservation is defined after JSON decoding;
2. why the unique constraint, rather than an application pre-check, arbitrates
   concurrent retries;
3. where the transaction starts and what remains after process death;
4. why the browser keeps a key only while retrying unchanged text;
5. which tests prove preservation, conflict, and concurrency behavior;
6. why no clarification or AI abstraction belongs in this slice.
