# Project Atlas

Project Atlas turns an unstructured curiosity into a question the learner can
act on and eventually prove with evidence.

한국어로 현재 결정과 다음 작업부터 보려면
[`docs/START_HERE.ko.md`](docs/START_HERE.ko.md)를 먼저 읽으세요.

The user does not need to arrive with a polished goal or a good search query.
Atlas first preserves what they meant, asks only the questions that materially
change the path, proposes an editable learning brief, and offers one small next
action. The first deep activity will be a backend incident lab with deterministic
verification; it is not part of the initial code slice yet.

## Current status

This repository contains the product and architecture baseline plus M1.1 Brain
Dump capture and M1.2 First Clarification Question. A local user can preserve
an unfinished thought, explicitly start clarification, and see one persisted
question with its reason beside the unchanged raw text. Creation and start are
retry-safe in PostgreSQL. The proposal is a deterministic in-process fake; no
live model, answer flow, Brief, or Question Map is implemented.

```text
apps/api   Java 21 + Spring Boot modular monolith
apps/web   React + TypeScript + Vite
docs       product, architecture, decisions, and AI change policy
```

## Read first

1. [`docs/START_HERE.ko.md`](docs/START_HERE.ko.md)
2. [`PRD.md`](PRD.md)
3. [`docs/TECH_SPEC.md`](docs/TECH_SPEC.md)
4. [`docs/M1_CAPTURE_SLICE.md`](docs/M1_CAPTURE_SLICE.md)
5. [`docs/M1_FIRST_CLARIFICATION_SLICE.md`](docs/M1_FIRST_CLARIFICATION_SLICE.md)
6. [`docs/M1_FIRST_CLARIFICATION_IMPLEMENTATION.md`](docs/M1_FIRST_CLARIFICATION_IMPLEMENTATION.md)
7. [`docs/FIRST_VERTICAL_SLICE.md`](docs/FIRST_VERTICAL_SLICE.md)
8. [`docs/AI_CHANGE_POLICY.md`](docs/AI_CHANGE_POLICY.md)
9. [`AGENTS.md`](AGENTS.md)

## Local prerequisites

- Java 21
- Node.js 22.17 or newer (Node 22 or 24)
- pnpm 10
- Docker Desktop

The repository uses Maven Wrapper, so a global Maven installation is not
required.

## Local setup

```powershell
docker compose up -d --wait postgres
pnpm install
```

The checked-in defaults work without a `.env` file. To override Compose values,
copy the example to the special filename that Compose reads automatically:

```powershell
Copy-Item .env.example .env
```

The API runs as a host process, so any API overrides must also be exported into
that terminal environment.

Run the API:

```powershell
Push-Location .\apps\api
.\mvnw.cmd spring-boot:run
Pop-Location
```

Run the web app in another terminal:

```powershell
pnpm --dir apps/web dev
```

Verify the repository:

```powershell
.\scripts\verify.ps1
```

## Product boundary

Atlas is not a generic chatbot, an automatic curriculum generator, or a claim
that an LLM can measure mastery. AI output is a proposal. The user approves
their learning state, and deterministic evidence decides anything that can be
tested objectively.
