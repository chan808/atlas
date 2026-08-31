# Local Development Launcher Contract

Status: implemented and verified
Date: 2026-09-01
Risk: local developer process orchestration; no deployment topology change
Independent review: accepted in
[`LOCAL_DEV_LAUNCHER_REVIEW.md`](LOCAL_DEV_LAUNCHER_REVIEW.md)

## User behavior

A Windows developer who has already started Docker Desktop can double-click
`dev.cmd`, or run `./dev.ps1`, to start the complete local Atlas stack:

1. the existing PostgreSQL Compose service;
2. the Spring Boot API in a visible terminal;
3. the Vite web application in a visible terminal;
4. the default browser at the local web URL after both applications are ready.

The PowerShell interface also supports `start`, `infra`, `status`, and `stop`.
Running without a command is equivalent to `start`.

## Non-goals

- Do not start or stop Docker Desktop itself.
- Do not containerize the API or web application.
- Do not add Redis, a broker, a cache, or another product service.
- Do not add production deployment, remote access, authentication, or TLS.
- Do not add a database reset, volume deletion, `down -v`, or destructive
  cleanup command.
- Do not make application correctness depend on the launcher.

## Invariants

- Existing direct API, web, Compose, and verification commands keep working.
- PostgreSQL data remains in the existing named volume after `stop`.
- `stop` may terminate only API and web process trees recorded by this launcher
  and still matching their recorded start time and launcher command.
- An already healthy API or web process is reused and never claimed as
  launcher-owned.
- Repeating `start` does not create duplicate API or web processes.
- Docker Compose resolves `.env` and shell precedence once; only an explicit
  Atlas allowlist from that resolved result is copied into child process
  environments. Values and secrets are not printed or written to the launcher
  state file.
- Runtime PID state stays under ignored `.dev/`; no generated state is
  committed.
- `status` and `stop` never start Docker or an application as a side effect.

## Failure behavior

- Missing Docker CLI, Java, pnpm, wrapper, or installed web dependencies fails
  before applications are launched, with an actionable message.
- An unavailable Docker Engine fails quickly and tells the user to start Docker
  Desktop; the launcher never attempts to start it.
- PostgreSQL health failure stops the start sequence before API or web launch.
- A busy API or web port that does not answer as Atlas fails instead of killing
  or replacing the unknown process.
- API or web readiness timeout leaves its visible terminal open for diagnosis
  and returns a non-zero exit code.
- After such a timeout, close the failed terminal or run `./dev.ps1 stop`
  before retrying; the launcher does not silently replace a still-running
  owned wrapper whose endpoint is unavailable.
- Stale or reused PID state is discarded and never used to kill a process.
- Closing one child terminal does not delete data or stop the other services.

## Expected file changes

- `dev.ps1`: command dispatch, prerequisites, Compose start, process ownership,
  readiness checks, browser launch, status, and safe stop.
- `dev.cmd`: double-click wrapper for `dev.ps1 start` that preserves failures.
- `apps/api/src/main/resources/application.yml`: expose the static,
  non-sensitive `project-atlas` identity through the already enabled Actuator
  info endpoint so the launcher cannot mistake another Spring Boot app for
  Atlas.
- `.gitignore`: ignore `.dev/` runtime state.
- `README.md` and `docs/START_HERE.ko.md`: make the launcher the convenient
  Windows path while retaining direct commands for diagnosis.
- `scripts/verify-dev-launcher.ps1` and `scripts/verify.ps1`: parse the script,
  assert required safety evidence and forbidden-command absence, and exercise
  read-only status in the normal repository verification path.

No product API, database schema, or dependency file changes. The existing
management info endpoint gains only the checked-in application identity.

## Verification plan

- Parse `dev.ps1` as PowerShell without executing it.
- Confirm `status` is read-only and reports Docker/PostgreSQL/API/web state.
- With Docker Desktop running, execute `infra` and verify PostgreSQL health.
- Execute `start -NoBrowser`, verify API `/actuator/health` and the Vite page,
  and repeat `start` to prove duplicate prevention.
- Execute `stop`, verify tracked API/web process trees stop and the database
  volume remains present.
- Run `scripts/verify.ps1` to prove product behavior is unchanged.
- Perform a separate review of process ownership, quoting, secret handling,
  failure cleanup, and destructive-command absence.

## Verification evidence

- Windows PowerShell 5.1 and PowerShell 7 parse the launcher and pass the
  launcher verifier.
- With Docker Desktop already running, `infra` started the existing PostgreSQL
  service and waited until healthy.
- `start -NoBrowser` opened API and web terminals and waited for Atlas-specific
  health, info, and page markers.
- A sequential repeated start preserved the exact state PIDs.
- Two truly concurrent starts were executed: one acquired the repository mutex,
  the other waited, and both completed with one recorded API wrapper, one
  recorded web wrapper, and one listener on each application port.
- A caller environment sentinel was restored after start; no allowlisted value
  remained changed in the invoking shell.
- `dev.cmd` completed with exit code `0`, reused healthy services, and reached
  the browser-open step.
- `stop` terminated only the recorded process trees, stopped PostgreSQL, and
  retained the `project-atlas_atlas-postgres-data` volume.
- `scripts/verify.ps1` passed the launcher verifier, web typecheck, 20 web
  tests, lint, production build, 33 PostgreSQL-backed API tests, Modulith
  verification, and API packaging.

The independent review initially blocked concurrent state claims, generic
endpoint identification, divergent `.env` parsing, and caller environment
leakage. Repository-scoped locking, Atlas-specific probes, Compose-resolved
allowlisting, UTF-8 response decoding, and `finally` restoration closed those
findings. The final review reported no remaining code finding.

## Rollback

Delete `dev.cmd`, `dev.ps1`, and this document, remove the `.dev/` ignore and
launcher documentation, then continue using the existing direct commands.
No data migration or product rollback is required. Runtime `.dev/` state may be
deleted only after verifying its resolved path is the repository-local `.dev`
directory; it contains no product data.
