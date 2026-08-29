# M1.2 Contract Independent Review

Status: accepted for implementation
Reviewed: 2026-08-30
Subject: `docs/M1_FIRST_CLARIFICATION_SLICE.md` and its product/architecture
cross-references

## Review boundary

A fresh-context review pass read the repository rules, PRD, M1.1 contract, M1
product flow, technical specification, AI change policy, roadmap, current M1.1
code, and the proposed M1.2 documentation diff. The reviewer did not implement
or edit the contract and did not rely on owner teach-back.

The review checked product and non-goal alignment, smaller alternatives, state
and transaction boundaries, idempotency, concurrency, process death,
migration/rollback behavior, privacy, test evidence, and unnecessary
dependencies or abstractions.

## Findings and resolutions

The first pass blocked approval. The contract was revised as follows:

1. The PostgreSQL idempotency claim, canonical request, turn, Inquiry
   compare-and-set, and loser reconciliation now have one explicit transactional
   contract. Rollback does not consume the key.
2. The V2 outcome now names the status-constraint replacement, version
   backfill, request and turn tables, relational uniqueness, and populated-V1
   preservation test.
3. The completion plan now proves raw-text equality across the new transition
   and captures logs from new conflict and failure paths for non-disclosure.
4. Web retries now distinguish ambiguous transport/`5xx` outcomes from terminal
   errors. A current-turn endpoint recovers the committed winner after a race.
5. Proposal text now has the same NUL and well-formed-Unicode safety boundary as
   persisted Brain Dumps, plus bounded source/schema identifiers.
6. The contract now states that M1.2 is positioned only for backend systems and
   reliability without adding a premature domain classifier.

A second pass found that `Location` could not safely point at the mutable
`current` alias. The contract added stable turn retrieval by `turnId`, retained
`current` only for discovery/recovery, and narrowed an inconsistent draft-API
statement in the M1 product contract.

## Decision

The final independent pass reported no remaining findings and approved the
M1.2 contract for implementation. It specifically confirmed that:

- all earlier transaction, migration, privacy, retry, Unicode, and domain-scope
  blockers were resolved;
- stable and current turn resources have distinct, forward-compatible roles;
- answers, later turns, Briefs, live AI, authentication, Question Maps, and new
  infrastructure remain outside the slice;
- no unnecessary dependency, generic agent abstraction, or new product module
  was introduced.

This approval applies to the contract only. The later implementation requires a
separate independent diff review, automated verification, and rollback handoff
before it can be marked complete.
