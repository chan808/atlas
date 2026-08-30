# M1.2 Implementation Independent Review

Status: accepted
Reviewed: 2026-08-30
Subject: M1.2 server, Flyway, HTTP, web, and automated test diff

## Review boundary

A separate review pass read the repository rules, the accepted M1.2 contract,
all tracked and untracked implementation files, and the new tests. The reviewer
did not edit the implementation and did not use owner teach-back as a gate.

The review traced proposal validation, transaction entry and rollback,
PostgreSQL claim/CAS behavior, same-key and different-key races, migration of a
populated V1 database, child ownership, safe HTTP errors, log non-disclosure,
and every web retry branch. It also checked that answer submission, Briefs,
Question Maps, live AI, authentication, Kafka, and later infrastructure were
not introduced.

## Blocking findings and resolutions

The first server pass blocked implementation approval with three findings:

1. A `null` proposal could enter the write transaction and fail only after the
   idempotency claim and compare-and-set. The application now rejects it as an
   invalid proposal before transaction entry, with a regression test.
2. Unexpected persistence failures did not have an explicit safe HTTP Problem
   Details boundary, and the rollback test bypassed HTTP. A general non-echoing
   500 handler and HTTP-path test now verify rollback plus raw text, key,
   question, and reason non-disclosure.
3. Independent foreign keys allowed a turn and its start request to reference
   different Inquiries. V2 now uses a composite ownership foreign key, and the
   populated migration test proves that a cross-owner insert is rejected.

The closure pass found no remaining server finding and independently ran all
33 API tests successfully.

## Web review

The web pass confirmed:

- capture response version is sent with the explicit start command;
- server-returned raw text remains exact and visually separate;
- no request starts before the labelled user action;
- an in-flight ref and disabled control prevent duplicate starts;
- manual transport/`5xx` retries reuse one key and version;
- `400` and `404` stop, while `409` performs exactly one current-turn read and
  never repeats the start command;
- failed conflict recovery is terminal;
- exactly one question and reason are displayed without answer or later-slice
  controls;
- error response bodies or private request values are not rendered.

The reviewer independently ran web typecheck, 20 tests, lint, and production
build successfully and reported no finding.

## Decision

Approved with no remaining findings. The approval covers only M1.2. It does not
approve deployment, authentication, live AI, answer handling, Briefs, or a
Question Map.
