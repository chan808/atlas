# Owner Learning Protocol

Atlas is both a product and the owner's long-term engineering curriculum. The
goal is not to read everything before building or to let AI build everything
before understanding it. Learning follows the next product decision.

## Weekly loop

1. Choose one user outcome and one unfamiliar concept.
2. Read the minimum official material needed to state the concept, trade-off,
   common failure, and observable signal.
3. Build a disposable spike when the concept is uncertain.
4. Write the change brief and implementation plan.
5. Let AI implement in small reviewable steps.
6. Run normal tests and one relevant failure drill.
7. Reproduce the feature from the user's point of view.
8. Explain the system without looking at the diagram.
9. Keep, simplify, or revert the decision based on evidence.

## Concept card

Before introducing a new technical concept, answer in one page or less:

- What concrete problem exists now?
- What guarantee does this concept add?
- What guarantee does it not add?
- How does it fail?
- How will Atlas observe the failure?
- What simpler alternative exists?
- How can the concept be removed?

If the current problem cannot be named, the technology stays in the scale lab or
learning notes rather than the product runtime.

## Understanding levels

- **Recognize:** identify the concept and vocabulary.
- **Explain:** describe the guarantee, boundary, and failure in your own words.
- **Apply:** implement it in a small controlled case.
- **Operate:** observe, diagnose, recover, and roll it back.

Critical production concepts are not considered learned until the owner reaches
`Operate`. Atlas product milestones should create the evidence for that level.

## Failure drills by area

- Transaction: crash before and after commit.
- Async work: duplicate delivery, lease expiry, worker death, timeout.
- Migration: old application with new schema, failed deploy, restore.
- Authentication: cross-user identifier access and expired credentials.
- Model provider: timeout, rate limit, invalid schema, unexpected cost.
- Runner: process death, orphan cleanup, denied network, resource exhaustion.
- Deployment: bad release, rollback, database compatibility, lost instance.

Only run drills relevant to implemented features.

## Learning log entry

At the end of a milestone, record:

- what the owner can now explain or operate;
- what assumption changed;
- one measured result;
- one failure reproduced;
- one remaining unknown;
- the next smallest learning question.

This log is evidence of engineering growth. It should not become a daily diary
or a copy of commit history.
