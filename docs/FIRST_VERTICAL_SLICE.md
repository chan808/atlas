# First Vertical Slice: Curiosity to an Approved Brief

Status: product contract for M1; implemented as independently reviewable slices

The first implemented engineering slice is **M1.1 Brain Dump capture**. Its
exact API, validation, idempotency, failure, and acceptance contract is
[`M1_CAPTURE_SLICE.md`](M1_CAPTURE_SLICE.md). M1.1 does not create a
clarification turn, proposal port, fake generator, Brief, or Exploration Action.

The next contracted but not implemented slice is **M1.2 First Clarification
Question**. Its exact state, proposal, API, retry, and acceptance contract is
[`M1_FIRST_CLARIFICATION_SLICE.md`](M1_FIRST_CLARIFICATION_SLICE.md). It ends
when the user sees one persisted question and its reason; answering that
question remains a later contract.

## Outcome

A local single user can paste an unstructured technical thought and, within ten
minutes, approve a concise statement that feels more accurate and actionable
than the original input. The user then receives one bounded exploration action.

This slice tests the product's riskiest assumption before adding search, RAG,
question graphs, incident runners, or distributed infrastructure.

## Example journey

### Brain Dump

> Kafka and large systems sound interesting, but at work I mostly make APIs and
> database queries. I have heard about retries and duplicate messages. I do not
> know whether I should learn Kafka first or what I am actually missing.

### Clarification

Atlas asks one question at a time. A question must say why the answer matters.

1. "What would you most like to do: explain the idea, implement a small system,
   or diagnose a failure?"
   Reason: the same topic needs a different path for conceptual, building, and
   operating goals.
2. "Which situation feels less clear: a request that never returns, or the same
   operation happening twice?"
   The user may answer `I do not know` or `both`.
3. "What result could you realistically make this week?"
   The user may request examples instead of inventing an answer.

The first slice stops after at most three answered questions. It does not keep
interviewing until the model claims certainty.

### Proposed Learning Brief

```text
Why now
You want to move beyond API-and-database work and understand failures that only
appear when multiple components communicate.

Desired capability
Given an uncertain request result, explain the risk of retry and design one
small example that prevents duplicate effects.

Current context
You have Spring Boot application experience. Your understanding of delivery,
retry, and idempotency has not been verified yet.

This slice excludes
Kafka operations, Kubernetes, and a full distributed-systems curriculum.

Evidence of useful progress
A sequence diagram plus a small example or test showing what happens when the
first operation succeeds but its response is lost.

Open assumptions
We have not confirmed whether you prefer design or implementation as the main
learning mode.
```

The user edits this text and explicitly approves it. Atlas never treats the
proposal as approved because the user clicked through clarification questions.

### First action

```text
Question
Does a timeout prove that the remote operation failed?

30-minute action
Draw client -> order service -> payment service. Mark the point where payment is
committed, drop only the response, then write what the client's retry can cause.

Completion result
One sequence diagram and three sentences explaining the ambiguity.

Stop condition
Stop after 30 minutes even if new terms appear. Capture the new terms as open
questions rather than expanding this action.
```

## User-visible states

1. `Capture`: large free-form input with optional starter prompts
2. `Clarify`: one question, its reason, and accessible alternative answers
3. `Review Brief`: editable structured fields with assumptions clearly marked
4. `Approve`: explicit confirmation and revision creation
5. `Next Action`: one question, action, result, time box, and stop condition
6. `Resume`: original reason and next action appear before a dashboard

Starter prompts may include:

- A term I keep seeing but cannot explain
- Something at work that would scare me during an incident
- Something I want to build someday
- A concept I thought I understood until I tried to explain it

## Functional requirements

### Capture

- Accept a value containing at least one non-whitespace character and no more
  than 10,000 Unicode code points. Short fragments and technology names are
  valid Brain Dumps.
- Preserve whitespace and wording in the stored raw value.
- Reject an empty or whitespace-only value.
- Do not send input to a live model in the fake-adapter milestone.
- Do not place the raw value in normal logs or error messages.

### Clarification

- Present one active question at a time.
- Store the reason for asking.
- Support free text, `DO_NOT_KNOW`, `BOTH`, and `SHOW_EXAMPLE`.
- Do not require additional text for `DO_NOT_KNOW`.
- End after no more than three answered questions in M1.
- Allow pause and resume after every answer.

### Brief

- Clearly distinguish user facts, system interpretation, and explicit
  assumptions.
- Allow editing every proposed field before approval.
- Preserve the raw Brain Dump next to, not inside, the proposal.
- Approval creates an immutable revision.
- Later editing creates a new draft revision.

### Exploration action

- Refer to the exact approved Brief revision.
- Contain one focus question.
- Fit a 30-minute time box.
- Name an observable completion result and stop condition.
- Avoid requiring a technology the Brief explicitly excludes.

## API contracts: implemented capture, contracted first question, and later drafts

The M1.1 create and retrieve endpoints are implemented and governed by
`docs/M1_CAPTURE_SLICE.md`. The M1.2 start and turn-retrieval endpoints are
contracted separately. The answer, Brief draft, approval, and later expanded
resume endpoints remain drafts that may be refined before their slice-level
implementation.

### Create Inquiry

```http
POST /api/inquiries
Idempotency-Key: <client-generated-key>
Content-Type: application/json

{ "rawText": "..." }
```

In M1.1, returns only the stored `CAPTURED` Inquiry. M1.2 keeps capture success
independent and adds a separate, explicit start request for the first
clarification proposal.

### Retrieve captured Inquiry

```http
GET /api/inquiries/{inquiryId}
```

In M1.1, returns only the same stored `CAPTURED` representation. Answered turns,
a Brief, and a next action do not exist yet.

### Start and retrieve the first clarification turn

M1.2 adds an explicit, idempotent start request, stable turn retrieval, and a
singleton current-turn recovery route after capture. The exact request,
response, optimistic version, transaction, retry, and error behavior is
governed by
[`M1_FIRST_CLARIFICATION_SLICE.md`](M1_FIRST_CLARIFICATION_SLICE.md). These
routes are contracted but not implemented.

### Answer clarification

```http
POST /api/inquiries/{inquiryId}/clarification-answers
Idempotency-Key: <client-generated-key>
Content-Type: application/json

{
  "turnId": "...",
  "answerKind": "FREE_TEXT | DO_NOT_KNOW | BOTH | SHOW_EXAMPLE",
  "text": "optional",
  "inquiryVersion": 1
}
```

Returns the next question or a proposed Brief when clarification is complete.

### Edit Brief draft

```http
PUT /api/inquiries/{inquiryId}/brief-draft
Content-Type: application/json

{
  "revision": 1,
  "motivation": "...",
  "desiredCapability": "...",
  "currentContext": "...",
  "excludedScope": ["..."],
  "expectedEvidence": ["..."],
  "openAssumptions": ["..."],
  "inquiryVersion": 3
}
```

### Approve Brief

```http
POST /api/inquiries/{inquiryId}/brief-approvals
Idempotency-Key: <client-generated-key>
Content-Type: application/json

{ "revision": 1, "inquiryVersion": 4 }
```

Returns the approved revision and first Exploration Action.

### Resume (later M1 draft)

```http
GET /api/inquiries/{inquiryId}
```

A later slice expands the M1.1 retrieve response with answered turns, the
current draft or approved Brief, and the next user-visible action. The response
does not expose internal prompts.

## Failure behavior

- Database failure during create returns an error and creates no partial Inquiry.
- The web app retains unsent Brain Dump text after a network or server error.
- A stale optimistic version returns `409 Conflict` with no mutation.
- Repeating a request with the same idempotency key returns the original result.
- Reusing an idempotency key with a different payload returns conflict.
- Proposal failure preserves the Inquiry and lets the user retry.
- Invalid structured output never changes state.

## Deterministic fake

The first clarification implementation, after M1.1 capture, uses a deterministic
proposal generator so that flow is testable without cost, network, or model
variance.

The fake may use a small set of reviewed clarification templates. It must still
follow the real port's constraints and return versioned structured output. It is
not presented to test users as production intelligence.

The live model adapter is a later milestone after:

- the data model and approval flow are stable;
- fake-backed dogfooding shows the flow is useful;
- evaluation examples exist;
- cost and privacy limits are implemented.

## Acceptance tests

- Whitespace-only Brain Dump is rejected.
- Unicode input is preserved exactly.
- The user can answer `DO_NOT_KNOW` without text.
- No fourth clarification question can be created in M1.
- The proposal cannot approve itself.
- Editing an approved Brief cannot mutate the approved revision.
- An action cannot reference an unapproved Brief.
- Duplicate creation and approval are idempotent.
- Stale concurrent edits are rejected.
- API failure does not clear web input.
- The primary flow is usable by keyboard and has explicit labels.
- Application logs do not contain fixture Brain Dump text.

## Non-goals

- Authentication or multiple workspaces; M1 is local-only
- Live AI provider
- Search, URL fetch, file upload, or RAG
- Question Map visualization
- Incident Lab or code execution
- Streaming tokens
- Social and gamification features
