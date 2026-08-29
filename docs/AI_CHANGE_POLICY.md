# AI-Assisted Change Policy

The project uses AI heavily for research, implementation, testing, and review.
Fast implementation is useful only when the resulting behavior remains
verifiable, reviewable, operable, and recoverable. The owner's current learning
progress does not gate development, commits, merges, or milestone completion.

## 1. Change evidence standard

Critical boundaries must be inspectable in code, tests, documentation, and
runbooks as appropriate:

- product state transitions and invariants;
- transaction and data ownership boundaries;
- authentication and authorization;
- migrations and recovery;
- timeout, retry, duplication, concurrency, and idempotency;
- AI execution, tools, cost, and privacy;
- process/container isolation;
- deployment, rollback, and observability.

Missing evidence for a critical change is treated like a failing test. The AI
must leave a concise handoff so the owner can learn the boundary now or later;
no owner explanation or confirmation is required before merge.

## 2. Complexity budget

One milestone may introduce one new architectural risk axis into the product
runtime. Familiar tools support that axis; they do not multiply it. This limits
production complexity, not what the owner may learn or when they learn it.

Examples:

- M1: aggregate state machine, not state machine plus Kafka plus Kubernetes
- M1.5: durable model execution, not a multi-agent framework and RAG
- M3: fault injection, not remote arbitrary-code execution
- M4: Runner isolation, not a full scheduler and multi-region platform

Experiments stay outside the product path until their benefit, failure modes,
and removal path are understood.

## 3. Change workflow

### Change brief

Before code, write or state:

- user behavior that changes;
- explicit non-goals;
- invariants;
- timeout, duplicate, concurrent, and partial-failure cases;
- files and boundaries expected to change;
- tests and observable completion criteria;
- rollback or removal path.

### Plan review

AI proposes the smallest vertical change. The plan is rejected when it adds
future abstractions, infrastructure without a trigger, or a second architectural
risk axis.

### Small implementation

- One observable behavior per change.
- Keep one implementation change in progress at a time.
- Parallel AI work is for research, test design, and independent review, not
  overlapping code edits.
- Core diffs must remain small enough for independent review and safe rollback.

### Independent review

A review pass with fresh context checks:

- requirement and non-goal violations;
- simpler alternatives;
- invalid state transitions;
- data leaks and authorization gaps;
- concurrency, retry, and migration behavior;
- tests that pass without proving the invariant;
- unnecessary dependencies or abstractions.

The implementation agent does not certify its own work.

### Verification

Run the full relevant suite. Do not change an assertion merely because generated
code fails it. A test change must explain why the previous behavior or invariant
was wrong.

### Change handoff

Before merge, the change record answers:

1. What user behavior changed?
2. Where is the state stored and where does the transaction end?
3. What remains if the process dies halfway through?
4. What happens on timeout, duplicate request, or retry?
5. Which test proves each critical invariant?
6. How is the change rolled back or removed?

The implementation and review agents may write this record. The owner may use
it as a learning guide in any later session; it is not a comprehension test or
approval gate.

## 4. Risk tiers

### Tier 0 — mechanical

Examples: formatting, generated mappings, static copy, simple CSS.

Required: review the resulting behavior and run relevant checks.

### Tier 1 — application behavior

Examples: controller mapping, normal validation, view state.

Required: user-flow explanation, tests, and failure behavior.

### Tier 2 — critical boundary

Examples: transaction, concurrency, migration, authorization, AI execution,
secrets, Runner, CI/CD, deployment.

Required: explicit change brief, independent review, automated verification,
rollback notes, and usually an ADR when the decision is costly to reverse.

## 5. ADR triggers

Create an ADR for a meaningful, hard-to-reverse decision involving:

- service or module boundaries;
- data ownership or storage technology;
- authentication or authorization model;
- public API compatibility;
- asynchronous delivery semantics;
- remote execution and isolation;
- deployment topology;
- a new infrastructure component.

Do not create an ADR for every pull request or implementation detail.

## 6. AI-specific safeguards

- Product state can only change through application use cases.
- Model output is untrusted and schema-validated.
- Models cannot approve Briefs, mark evidence demonstrated, or forge verifier
  results.
- Models receive no shell, URL-fetch, database-mutation, or code-execution tool
  in the active milestones.
- Provider SDKs stay behind product-defined ports.
- Prompt and output schema versions are recorded when a live adapter arrives.
- CI never depends on a paid model or production key.
- User input, prompts, and model output are not written to normal logs.
- Cost, concurrency, and token limits precede external live-model access.

## 7. Stop conditions

Stop feature work and pay down evidence or architecture debt when:

- two undocumented critical changes have accumulated;
- the current request, data, and transaction flow is absent from code or docs;
- tests are edited without recording which invariant changed;
- a feature touches modules indiscriminately;
- generated abstractions or duplicate code keep increasing;
- docs and architecture tests disagree;
- one full milestone adds infrastructure but no user outcome;
- a component has no measured benefit or removal plan.

## 8. Required evidence per milestone

- one executable user outcome;
- passing verification;
- a concise change handoff for critical boundaries;
- an ADR only when a decision warrants it;
- real usage observations and measurements;
- a rollback, deletion, or simplification decision.
