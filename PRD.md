# Project Atlas Product Requirements

Status: product hypothesis v0.1
Initial domain: backend systems and reliability
Long-term vision: curiosity-to-evidence learning system

## 1. Decision summary

Project Atlas helps a person who is curious but cannot yet express a good goal,
question, or search query.

> The user can dump an unstructured thought. Atlas preserves the original words,
> asks a few high-value questions, proposes an editable learning brief, and
> connects one answerable question to one concrete next action and eventually to
> evidence of understanding.

Atlas is neither a generic AI learning assistant nor only an incident-training
product.

- The product core is turning ambiguity into a user-approved learning direction.
- The first specialist activity is a deterministic backend incident lab.
- The long-term vision may support other domains, but the current product does
  not claim to teach everything.

## 2. Why this product should exist

Many people do not abandon learning because information is unavailable. They
abandon it before they can form a useful question.

### Intent gap

- They recognize a few terms but cannot explain what interests them.
- They do not know what they do not know.
- They cannot choose between understanding, building, or operating something.
- A broad goal such as "learn distributed systems" is too large to act on.

### Structure gap

- Search results do not reveal prerequisites or conceptual relationships.
- Every answer introduces more terms and more tabs.
- The learner cannot tell which disagreement matters.
- Planning becomes another task, so the learning never starts.

### Action gap

- Reading feels like understanding but produces no observable capability.
- An AI-generated answer can be copied without becoming the learner's knowledge.
- A tutorial may work without showing which guarantee was established.

### Continuity gap

- After a break, the learner forgets why a source or task mattered.
- They must reconstruct the plan before they can continue.
- The cost of resuming becomes larger than the next learning action.

## 3. Initial user

The first user is deliberately narrow.

- A working backend developer, initially familiar with Java and Spring Boot
- Curious about distributed systems, messaging, Kubernetes, observability, and
  production operations
- Knows some vocabulary but cannot yet connect the concepts
- Has repeatedly started articles, courses, or AI chats and then stopped
- Wants practical design and operating ability rather than only a certificate
- Wants code, decisions, experiments, and incident reports that can become a
  portfolio

The founder is the first user and dogfoods every supported path.

The current milestone does not target programming beginners, expert SREs, or
arbitrary non-technical subjects.

## 4. Jobs to be done

### Primary job

> When I am interested in a technical area but cannot say exactly what I need to
> learn, help me turn that vague interest into a question and a small action
> before searching exhausts me.

### Supporting jobs

- Help me notice assumptions and unknowns I could not name.
- Explain why one question should come before another.
- Let me say "I do not know" without ending the flow.
- Let me edit what the AI thinks I meant before it becomes my plan.
- Preserve the reason, context, and next action so I can resume quickly.
- Require evidence before claiming I understand something.
- Let me use AI while still proving that I own the result.

## 5. Product principles

1. **Not knowing is valid input.** The user does not prepare a goal or search
   query before using Atlas.
2. **Preserve before interpreting.** The raw brain dump is immutable; summaries
   are separate, versioned proposals.
3. **Ask fewer, consequential questions.** A clarification question is allowed
   only when its answer can change scope, direction, or expected evidence.
4. **One question at a time.** Do not replace uncertainty with a long intake
   form.
5. **The user approves meaning.** AI cannot silently turn a proposal into the
   learner's goal or question map.
6. **Prefer one next action over a grand curriculum.** Atlas should reduce
   activation energy, not produce an intimidating plan.
7. **Evidence beats confidence.** Do not display invented mastery percentages.
8. **Deterministic checks beat AI judgement.** If a property can be tested, the
   test decides it.
9. **AI should expose gaps, not remove productive struggle.** Hints and questions
   come before full solutions.
10. **Continuity starts with context.** A returning user first sees why the work
    mattered and what to do next.

## 6. Product vocabulary

### Brain Dump

The user's original, unstructured words. It may contain fragments, conflicting
goals, names of technologies, fears, and "I do not know" statements.

### Clarification Turn

One question whose answer can materially change the learning direction. Every
turn records why the question was asked. `I do not know`, `both`, and `show an
example` are first-class responses.

### Learning Brief

An editable statement of:

- why the user cares now;
- what they want to be able to do;
- the current context without pretending to measure mastery;
- constraints and deliberately excluded scope;
- open assumptions;
- the evidence that would show useful progress.

### Question Map

A small, versioned hierarchy of answerable questions, prerequisites, and
expected evidence. The first UI is a readable outline, not a graph visualization.

### Expedition

A bounded learning journey for one focus question.

### Activity

An action inside an Expedition, such as explaining a concept, comparing designs,
building a small artifact, or completing an incident lab.

### Evidence

An immutable submission that supports a claim of understanding: explanation,
design, code, test report, trace, decision record, or postmortem.

### Evidence Pack

A shareable bundle of the question, decisions, artifacts, verification results,
and reflection.

## 7. Core product loop

```text
unstructured curiosity
  -> one clarification question at a time
  -> editable Learning Brief proposal
  -> explicit user approval
  -> small Question Map
  -> one recommended next action
  -> evidence
  -> deterministic verification where possible
  -> reflection
  -> next question or stop
```

### Example

Input:

> Kafka is interesting and I want to learn large systems, but at work I mostly
> make APIs and database queries. I have heard about retries and duplicates but
> I do not know what I am actually missing.

Approved focus:

> When a request times out, how can I safely retry without assuming that the
> original operation failed?

First deep activity:

> Reproduce a lost response that causes a duplicate payment, form a hypothesis,
> change the system, and prove under retry and restart that one order creates at
> most one charge.

## 8. Scope by milestone

### M0: repository and contract

- Product, architecture, and AI change documents
- Spring Boot and React workspace
- PostgreSQL local environment
- Tests and CI baseline
- No product functionality yet

### M1: minimum service

The first vertical slice ends after the user approves a Learning Brief and sees
one 30-minute exploration action.

- Free-form Brain Dump
- At most three initial clarification turns, one at a time
- `I do not know`, `both`, and `show an example` responses
- Editable Learning Brief proposal
- Explicit approval that creates an immutable revision
- One bounded next action
- Pause and resume without losing the original words or answers
- Deterministic fake proposal generator in CI and local development

M1 does not generate a Question Map or run a lab. It tests whether Atlas can make
the user say, "Yes, that is what I was trying to understand."

### M2: question to action

- A 5-9 node Question Map inside the supported backend domain map
- User approval and versioning of the map
- One recommended Expedition with a reason
- Simple text or design evidence
- Reflection and next-action update

### M3: first deep activity

- One version-pinned incident lab: lost response and duplicate payment
- Reproducible fault scenario
- User hypothesis before hints
- Deterministic invariant checks
- AI feedback that cites submitted evidence but does not issue the verdict
- Evidence Pack

M1 through M3 together form the first product MVP. They are not implemented as a
single engineering milestone.

## 9. First incident lab contract

The first incident teaches ambiguous outcomes, retry, idempotency, persistence,
and evidence-based reasoning.

The deterministic verifier eventually checks:

- one payment per order;
- zero duplicate payments under lost responses and retry;
- the guarantee survives process restart;
- every order reaches a terminal state within the scenario deadline;
- the failing baseline fails and the known-good solution passes.

AI may ask for a hypothesis, expose a relevant signal, challenge an explanation,
or propose a counterexample. It cannot mark the lab as passed.

## 10. Explicit non-goals

The current product does not include:

- an open-ended general chatbot;
- arbitrary subjects outside the supported backend domain;
- unlimited web search, crawling, or file ingestion;
- a complete course or eight-week curriculum generated in one request;
- RAG over personal notes;
- autonomous multi-agent conversations;
- lectures or video hosting;
- scores, levels, leaderboards, or certificates;
- community feeds or real-time study rooms;
- user-authored labs or a marketplace;
- cloud execution of arbitrary user code or images;
- Kafka, Kubernetes, Redis, OpenSearch, a graph database, or a workflow engine
  without a measured product need.

## 11. Differentiation hypothesis

Atlas does not compete on having the smartest model. Its hypothesis is that the
following combination is useful:

- clarification before search;
- a user-approved and persistent statement of intent;
- a small question map backed by a reviewed domain map;
- one action chosen for a reason the user can see;
- objective evidence rather than self-reported time or AI confidence;
- continuity from one unresolved question to the next.

The defensible assets, if the product works, are the reviewed domain maps,
versioned activities and verifiers, evidence history, and the accumulated links
between questions and effective actions.

## 12. Success and falsification

### M1 product proof

Test first with the founder, then 5-10 matching developers.

- At least 70% approve a useful Brief within ten minutes.
- At least 70% rate "Atlas expressed something I could not clearly say" at 4/5
  or higher.
- At least 60% begin the proposed 30-minute action within 24 hours.
- A returning user can identify why they started and what comes next within two
  minutes.

### MVP product proof

After M3, test with 20-30 matching developers.

- At least 60% of activated users start the incident lab.
- At least 40% of starters complete it within seven days.
- At least 60% of completers explain the cause and guarantee without copying a
  generated answer.
- At least 30% save or share the Evidence Pack.
- At least 20% request or start a second Expedition.

Message counts, generated document counts, and time spent are not north-star
metrics. The initial north star is **weekly completed learning loops with
evidence**.

### Pivot rules

- If users skip the Brief and only value the lab, narrow the product to an
  Incident Lab.
- If users value the Brief but do not start activities, reduce the action size
  before adding more planning features.
- If the Brief is not materially better than a normal AI chat after two UX
  iterations, remove or redesign the clarification hypothesis.
- If neither the question flow nor activity is preferred over AI chat, search,
  and a local project, stop product development or retain it only as an
  engineering portfolio.

## 13. Long-term direction

Only after the backend domain loop is validated may Atlas consider:

- additional incident scenarios;
- shared question maps and study cohorts;
- a remote isolated runner;
- other engineering domains;
- broader project-based learning;
- general learning domains with human-reviewed maps and objective activities.

General learning is a vision, not permission to build generic abstractions now.
