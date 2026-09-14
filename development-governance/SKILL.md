---

name: development-governance
description: >
Governs implementation work by keeping project documentation, GitHub Issues,
GitHub Projects, and the internal Sprint Plan synchronized with actual
development. Use this skill during feature implementation, bug fixing,
refactoring, architectural changes, or any coding task where implementation
may diverge from documented requirements. Require explicit user approval
before accepting undocumented changes, reflect approved changes in the
relevant documentation, update GitHub Issues and GitHub Projects as work
progresses, and keep the project's Sprint Plan synchronized with actual
development status.
compatibility: >
Intended for Codex working inside a Git repository. Requires access to project
documentation and Git. GitHub synchronization requires GitHub CLI (`gh`) or
equivalent GitHub integration with permission to read and update Issues and
Projects.
metadata:
version: "1.0.0"
category: "development-governance"
----------------------------------

# Development Governance

## Purpose

Use this skill as the governance layer for implementation work.

The project documentation is the source of truth for intended behavior,
requirements, architecture, interfaces, constraints, acceptance criteria, and
planned work.

Implementation, documentation, GitHub Issues, GitHub Projects, and the internal
Sprint Plan must remain synchronized.

This skill enforces three invariants:

1. No undocumented implementation change may silently become part of the
   project.
2. GitHub Issues and GitHub Projects must reflect the real implementation
   state.
3. The internal Sprint Plan must reflect the real implementation state.

Do not treat synchronization as an optional cleanup activity at the end of the
task. Perform it continuously at meaningful development checkpoints.

# Core Rules

## Rule 1 — Documentation Is the Source of Truth

Before implementing a task, identify the documentation governing that task.

Relevant documents may include, but are not limited to:

* requirements;
* feature specifications;
* architecture documents;
* ADRs;
* API specifications;
* domain models;
* database specifications;
* technical design documents;
* acceptance criteria;
* Sprint Plan;
* README or developer documentation.

Read the relevant documentation before making implementation decisions.

Do not assume that the current implementation supersedes documented
requirements merely because the code differs from the documentation.

When code and documentation disagree, explicitly identify the discrepancy.

# Rule 2 — Undocumented Change Requires User Approval

During implementation, continuously compare implementation decisions against the
documented specification.

A change requires user approval when it introduces or modifies something that
is not already authorized by the relevant documentation.

Examples include:

* requirement changes;
* scope changes;
* observable behavior changes;
* business rule changes;
* architecture changes;
* module or service responsibility changes;
* public API changes;
* request or response contract changes;
* database schema changes;
* domain model changes;
* event or message contract changes;
* dependency changes with architectural or operational impact;
* security or authorization behavior changes;
* persistence strategy changes;
* external integration changes;
* configuration requirements;
* deployment assumptions;
* acceptance criteria;
* implementation constraints documented by the project.

Routine implementation details that do not alter documented behavior,
interfaces, architecture, constraints, or acceptance criteria do not require an
approval checkpoint unless the project's documentation explicitly requires
them.

When uncertain whether a change is material, treat it as material and request
approval.

# Change Approval Gate

When an undocumented or conflicting change is discovered:

1. Stop work on the affected part of the implementation.
2. Do not silently choose an alternative.
3. Explain the discrepancy to the user.
4. Identify why the documented approach cannot or should not be followed.
5. Describe the proposed change.
6. Describe its impact.
7. Present reasonable alternatives when applicable.
8. Ask the user for explicit approval.
9. Do not proceed with the affected change until approval is received.

Use a concise approval request containing:

**Documented behavior**

What the existing documentation currently requires.

**Observed constraint**

What was discovered during implementation.

**Proposed change**

What should be changed.

**Impact**

Which components, interfaces, tests, requirements, documents, or planned tasks
are affected.

**Decision required**

Explicitly state that user approval is required before proceeding.

Do not interpret silence as approval.

Do not infer approval from previous unrelated decisions.

Approval must apply to the specific proposed change.

# After Approval

Once the user approves the change:

1. Update the relevant source-of-truth documentation.
2. Update related acceptance criteria if necessary.
3. Update architectural or API documentation if affected.
4. Update the Sprint Plan if scope, estimates, dependencies, or deliverables
   changed.
5. Update the related GitHub Issue when the decision affects its scope or
   implementation.
6. Update GitHub Project metadata when appropriate.
7. Resume implementation based on the newly approved specification.

Prefer updating documentation before implementing the approved deviation so
that implementation follows the new source of truth rather than temporarily
diverging from it.

# Rule 3 — GitHub Issue Synchronization

Identify the GitHub Issue associated with the current implementation task.

Use repository context, branch names, commits, task descriptions, linked
documents, or GitHub metadata to determine the relevant issue.

Do not create duplicate issues when an appropriate issue already exists.

If no suitable issue exists and the work represents independently trackable
development work, create one when GitHub access is available.

Each issue should accurately represent:

* objective;
* scope;
* relevant requirements;
* acceptance criteria;
* implementation status;
* important implementation decisions;
* discovered blockers;
* approved scope changes;
* remaining work.

Avoid flooding an issue with low-value implementation logs.

Record meaningful state transitions and decisions instead.

# GitHub Issue Status Updates

Update the associated issue at meaningful checkpoints such as:

* implementation started;
* significant subtask completed;
* blocker discovered;
* specification change approved;
* implementation completed;
* tests completed;
* review required;
* task completed.

When posting progress updates, prefer concise structured information:

### Progress

What has been completed.

### Current

What is currently being implemented.

### Decisions

Important approved decisions since the previous update.

### Blockers

Anything preventing progress.

### Remaining

Meaningful remaining work.

Omit sections that contain no useful information.

# Rule 4 — GitHub Projects Synchronization

When the repository uses GitHub Projects, ensure the relevant Issue or Pull
Request is represented in the appropriate project.

Discover the project's existing workflow before modifying fields.

Do not assume field names or allowed values.

Inspect the project first.

Typical fields may include:

* Status;
* Sprint;
* Iteration;
* Priority;
* Size;
* Estimate;
* Start Date;
* Target Date;
* Feature;
* Epic.

Respect the project's existing taxonomy.

Do not create new project fields merely because a preferred field does not
exist.

Synchronize project state whenever the actual development state meaningfully
changes.

For example:

`Todo -> In Progress`

when implementation begins.

`In Progress -> Blocked`

when work cannot continue because of an unresolved dependency or user decision,
if the project supports a Blocked state.

`In Progress -> In Review`

when implementation is complete but review or validation remains, if supported.

`In Review -> Done`

only when the project's completion criteria are satisfied.

Never mark work as `Done` merely because code has been written.

# GitHub Tool Usage

Prefer the GitHub CLI when available.

Useful command families include:

```bash
gh issue
gh pr
gh project
gh api
```

Before performing write operations:

1. Confirm the current repository.
2. Determine the repository owner.
3. Determine the relevant issue.
4. Determine the relevant GitHub Project.
5. Inspect available project fields and allowed values.

Do not hardcode project IDs, field IDs, option IDs, repository owners, or issue
numbers unless explicitly configured by the project.

Discover them from the current environment whenever possible.

If GitHub authentication or permissions are unavailable, do not fabricate a
successful synchronization.

Instead:

1. continue local work when doing so does not violate another governance rule;
2. record the pending synchronization;
3. tell the user which GitHub updates could not be performed.

# Rule 5 — Sprint Plan Synchronization

Locate the project's Sprint Plan before starting implementation.

Common locations may include:

```text
docs/SprintPlan.md
docs/sprint-plan.md
docs/project/sprint-plan.md
planning/SprintPlan.md
SPRINT_PLAN.md
docs/scrum-plan
```

Do not assume one of these paths exists.

Search the repository.

Preserve the project's existing Sprint Plan format instead of replacing it with
a new structure.

The Sprint Plan should accurately reflect:

* planned tasks;
* current status;
* completed work;
* remaining work;
* blockers;
* dependencies;
* approved scope changes;
* relevant issue references;
* relevant pull request references;
* significant decisions when appropriate.

# Sprint Plan Status

Use the project's existing status vocabulary.

If no vocabulary exists, prefer:

* `TODO`
* `IN PROGRESS`
* `BLOCKED`
* `DONE`

Only mark a Sprint Plan item as `DONE` when its applicable completion criteria
are satisfied.

Implementation alone does not necessarily constitute completion.

Tests, documentation, migration work, review, or other acceptance criteria may
still remain.

# Development Workflow

For every implementation task, execute the following workflow.

## Phase 1 — Establish Context

Before modifying code:

1. Inspect repository instructions such as `AGENTS.md`.
2. Identify relevant project documentation.
3. Locate the Sprint Plan.
4. Determine the relevant Sprint item.
5. Determine the relevant GitHub Issue.
6. Determine the relevant GitHub Project item.
7. Read the requirements and acceptance criteria.
8. Inspect the existing implementation.
9. Compare the requested task against documented scope.

Build an internal understanding of:

```text
Documentation
      |
      v
Sprint Plan
      |
      v
GitHub Issue / Project
      |
      v
Implementation
```

All four representations should describe the same underlying work.

## Phase 2 — Start Work

When beginning implementation:

1. Set the corresponding Sprint Plan item to the appropriate active state.
2. Set the GitHub Issue/Project item to the appropriate active state when
   applicable.
3. Record meaningful starting context if the project workflow expects progress
   comments.
4. Begin implementation.

Avoid making empty status-only document commits unless the repository workflow
explicitly expects them. Group synchronization changes with meaningful
development checkpoints where practical.

## Phase 3 — Implement and Continuously Validate

During implementation, repeatedly evaluate:

```text
Does this implementation still conform to the documented specification?
```

If yes, continue.

If no, execute the Change Approval Gate.

Do not postpone known specification discrepancies until task completion.

## Phase 4 — Synchronize Meaningful Progress

After a meaningful implementation milestone:

1. Evaluate whether the GitHub Issue requires a progress update.
2. Evaluate whether GitHub Project status or metadata changed.
3. Update Sprint Plan progress.
4. Ensure approved decisions have been reflected in documentation.

A meaningful milestone may be:

* completion of a significant subtask;
* completion of a vertical slice;
* completion of a module;
* resolution of a blocker;
* approval of a specification change;
* completion of implementation;
* completion of tests.

Do not generate noisy updates for trivial file modifications.

## Phase 5 — Validate Completion

Before declaring the task complete:

1. Run applicable tests.
2. Run applicable linting or static analysis.
3. Verify acceptance criteria.
4. Compare implementation against documentation.
5. Check for undocumented behavior changes.
6. Check GitHub Issue status.
7. Check GitHub Project status.
8. Check Sprint Plan status.
9. Verify that approved changes are documented.

Perform a final consistency check:

```text
Specification == Implementation
Implementation Status == GitHub Issue
GitHub Issue Status == GitHub Project
Development Status == Sprint Plan
```

The representations do not need identical wording, but they must not
contradict one another.

# Completion Gate

Do not mark a development task complete until all applicable conditions below
are satisfied:

* implementation is complete;
* applicable tests pass;
* acceptance criteria are satisfied;
* no unresolved undocumented implementation deviation exists;
* every approved specification change is documented;
* GitHub Issue reflects the current state;
* GitHub Project reflects the current state;
* Sprint Plan reflects the current state;
* unresolved blockers are explicitly recorded.

If any applicable condition is false, report the task as incomplete.

# Handling Blockers

When blocked by a technical issue that does not require specification changes,
record the blocker and continue with independent work when possible.

When blocked because the specification must change, execute the Change Approval
Gate.

When waiting for user approval:

* do not implement the disputed change;
* independent unaffected work may continue;
* mark the affected task or subtask as waiting for decision or blocked when the
  project's workflow supports it;
* record the pending decision in the relevant Issue and Sprint Plan when it
  materially affects development status.

# Scope Expansion

If implementation reveals additional work outside the current task:

Do not silently absorb substantial new work into the current task.

Determine whether it represents:

1. necessary work already implied by the documented requirement;
2. a new subtask;
3. a new issue;
4. a scope change.

If it is a scope change, obtain user approval.

If it is independently trackable work, create or propose a separate GitHub
Issue and reflect the dependency in the Sprint Plan.

# Documentation Update Principles

When updating documentation:

* modify the authoritative document rather than duplicating the same
  specification elsewhere;
* preserve existing terminology;
* preserve existing structure where practical;
* explain decisions rather than merely describing code;
* update diagrams when architectural relationships change;
* update examples when contracts change;
* update acceptance criteria when expected behavior changes;
* maintain links between Sprint items and GitHub Issues when the project already
  uses such links.

Do not create documentation solely to make implementation appear compliant.

Documentation must describe the intended project state.

# Git Commit Discipline

When appropriate, include related documentation synchronization with the
implementation that required it.

Do not commit a specification-changing implementation while leaving its
approved documentation update for an unspecified later task.

Before committing, inspect the diff and verify that no accidental unrelated
changes are included.

Follow repository-specific commit conventions from `AGENTS.md` or other project
instructions.

# Conflict Resolution

When information conflicts, use the following precedence unless the project
explicitly specifies otherwise:

1. explicit current user decision;
2. approved project specification;
3. project architecture and design documentation;
4. Sprint Plan;
5. GitHub Issue;
6. GitHub Project metadata;
7. existing implementation.

Existing code is evidence of current behavior, not automatically the desired
behavior.

If two authoritative documents conflict and the correct interpretation cannot
be determined safely, ask the user for a decision rather than arbitrarily
choosing one.

# Final Report

At the end of an implementation task, provide a concise governance summary.

Use this structure when applicable:

```markdown
## Implementation

- Completed:
- Tests/validation:

## Documentation

- Updated:
- Approved changes incorporated:

## GitHub

- Issue:
- Project status:

## Sprint Plan

- Updated item:
- Current status:

## Outstanding

- Blockers:
- Pending decisions:
- Remaining work:
```

Do not claim a GitHub or documentation update unless it was actually performed.

Do not include empty sections when nothing relevant exists.

# Mandatory Behavioral Constraints

Never silently change documented requirements.

Never use implementation convenience as implicit authorization to alter the
specification.

Never mark an item complete merely because code generation has finished.

Never fabricate GitHub updates.

Never fabricate test results.

Never fabricate user approval.

Never treat an outdated implementation as stronger evidence than an approved
specification.

Never defer known documentation synchronization until an unspecified future
time.

Never create unnecessary issue comments or Sprint Plan churn for trivial code
changes.

Always keep user-approved decisions traceable from requirement to
implementation.

# Final Consistency Checklist

Before finishing any task governed by this skill, verify:

* [ ] Relevant documentation was identified and read.
* [ ] Sprint Plan was identified.
* [ ] Relevant GitHub Issue was identified or its absence was handled.
* [ ] Relevant GitHub Project was identified when applicable.
* [ ] Implementation conforms to documented requirements.
* [ ] Every material deviation received explicit user approval.
* [ ] Every approved deviation was reflected in documentation.
* [ ] GitHub Issue reflects actual development state.
* [ ] GitHub Project reflects actual development state.
* [ ] Sprint Plan reflects actual development state.
* [ ] Tests and acceptance criteria were validated where applicable.
* [ ] Remaining blockers and pending decisions are explicit.

Only report the task as fully complete when every applicable check passes.
