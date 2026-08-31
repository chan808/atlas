# Local Development Launcher Independent Review

Status: accepted
Reviewed: 2026-09-01
Subject: `dev.ps1`, `dev.cmd`, launcher verification, management identity, and
local-run documentation

## Review boundary

A separate review pass read the repository rules, AI change policy, launcher
contract, implementation diff, and verification evidence. The reviewer did not
edit the implementation. It checked concurrent invocation, PID reuse, process
ownership, command quoting, secret lifetime, Compose environment semantics,
port identity, partial failure, Docker behavior, destructive commands, and
Windows PowerShell compatibility.

## Blocking findings and resolutions

1. Concurrent `start` calls could both observe empty state and leave an
   untracked wrapper. A repository-identity named mutex now serializes `start`,
   `infra`, and `stop`, handles abandoned ownership, releases in `finally`, and
   has a bounded wait. A real two-process race converged on one API and one web
   wrapper.
2. A generic successful HTTP response could be mistaken for Atlas. Web
   readiness now requires the checked-in Project Atlas title; API readiness
   requires both health `UP` and the static `project-atlas` identity from the
   existing Actuator info endpoint. Byte responses are explicitly decoded as
   UTF-8 before matching.
3. A custom `.env` parser could disagree with Docker Compose quoting,
   interpolation, and comments. The launcher now consumes Compose's resolved
   environment and copies only the Atlas allowlist without printing values.
4. Allowlisted values could remain in an interactive caller after the script
   ended. Start now snapshots the caller environment and restores every value
   in `finally`; a sentinel test confirms the behavior. `infra` performs no
   environment copy.

## Confirmed boundaries

- Docker Desktop is never started or stopped by the launcher.
- Compose controls only the existing PostgreSQL service.
- PID, start time, and launcher role command line must all match before a
  process tree can be terminated.
- No broad image/name kill, recursive cleanup, Compose `down`, volume deletion,
  Redis, application container, or new dependency is present.
- Secrets are absent from output, command lines, and `.dev/processes.json`.
- `dev.cmd` preserves a visible failure message for double-click use.
- Windows PowerShell 5.1 and PowerShell 7 syntax and invocation paths pass.

## Decision

Approved with no remaining code finding. This approval covers local developer
orchestration only; it does not approve deployment, remote exposure,
authentication, or reboot-time service supervision.
