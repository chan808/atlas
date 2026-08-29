# ADR 0001: Use a question-to-evidence product shape

Status: accepted
Date: 2026-08-28

## Context

The initial idea was a broad AI learning operating system. It matched the desire
to organize learning but risked becoming an AI chat, planning, and note wrapper.
It also did not naturally justify the distributed systems and operational depth
the project owner wants to learn.

A pure incident-lab product provided objective verification and natural systems
complexity, but it skipped the earlier problem: many users cannot yet say what
they need to learn or why a particular lab matters.

## Decision

Atlas will turn an unstructured curiosity into an approved question and a
concrete action, then require evidence before claiming progress.

The initial domain is backend systems and reliability. A deterministic Incident
Lab is the first deep Activity, not the whole product and not the first code
milestone.

M1 implements only Brain Dump, clarification, editable Learning Brief approval,
and one exploration action. Question Maps and Incident Lab arrive in later
milestones after the earlier loop is validated.

## Consequences

- The product can begin from "I do not know what I am asking."
- The initial user and content domain remain narrow enough to evaluate quality.
- AI interpretation is separated from user approval.
- Incident activities can provide objective evidence and a strong portfolio.
- The MVP is larger than M1, so milestone boundaries must be enforced.
- General-learning expansion cannot drive current abstractions.

## Alternatives rejected

### Generic AI learning OS

Rejected as the first product because differentiation, evaluation, retention,
and the need for distributed architecture were too weak.

### Pure Incident Lab

Rejected as the whole product because it assumes the learner already knows
which capability or incident they should practice.

### AI code-ownership reviewer

Useful as a later capability, especially for teach-back, but too narrow as the
primary learning experience and potentially uncomfortable as a team gatekeeper.

## Revisit when

- Users consistently skip clarification and only value labs.
- Users value the Brief but do not begin the proposed action.
- The complete flow is not preferred to normal AI chat, search, and local work.
