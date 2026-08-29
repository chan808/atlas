# ADR 0002: Start with a modular monolith and PostgreSQL

Status: accepted
Date: 2026-08-28

## Context

The long-term product could require asynchronous model work, isolated lab
runners, real-time telemetry, resource scheduling, and event replay. None of
those requirements have been validated in the first curiosity-to-Brief flow.

Starting with several services or infrastructure systems would increase
operational and comprehension cost before producing evidence that the product is
useful.

## Decision

The first deployment is a React web app, one Spring Boot modular monolith, and
one PostgreSQL database.

Only implemented product capabilities become modules. M1 creates `inquiry`; it
does not scaffold future learning, activity, execution, AI, or incident modules.
Spring Modulith tests verify boundaries as modules appear.

PostgreSQL owns transactional state and later may hold a small durable job queue.
Flyway is the only schema migration mechanism.

## Consequences

- The full request and transaction flow is easy to explain and test.
- Features can be delivered without distributed failure modes that do not yet
  serve users.
- Module boundaries and table ownership must still be explicit.
- A future Runner will be a separate process because it is a security boundary,
  not because of a microservice target.
- Scaling technologies can be explored in a separate scale lab without entering
  the product runtime.

## Alternatives rejected

### Microservices from the start

Rejected because no independent scale, security, team ownership, or deployment
lifecycle currently requires them.

### Serverless functions for each use case

Rejected because they would fragment transactional state and local development
before their scaling or cost benefits are known.

### Kafka and event sourcing

Rejected because M1 needs explicit state transitions and audit history, not a
replay platform or event-sourced aggregate.

## Revisit when

A measured security boundary, independent scaling requirement, deployment
lifecycle, or PostgreSQL throughput limit makes the current deployment a real
constraint. The relevant trigger and before/after measurements must be recorded
in a new ADR.
