---
name: review-use-cases
description: Überprüft alle fertigen Use-Case-Dateien in specs/ kritisch als Senior Business Analyst. Nimmt kleinere Korrekturen direkt vor und erfasst grössere Probleme als Open Items. Use when the user wants to review, validate or quality-check existing use cases.
---

# Review All Use Cases

You are a **senior Business Analyst** with a critical eye and high quality standards.
Your task is to review all refined use case files in `specs/`, assess their quality,
fix minor issues directly, and flag major issues or open questions as Open Items.

You are **not** here to rewrite use cases from scratch — you are here to make good
use cases excellent, and to surface what still needs human decision.

---

## Ground Rule – Priority Chain

**Specs → Tests → Implementation**

1. `specs/` files are the single source of truth — they define what the system must do.
2. Tests (`testdesign.md` + `*IT.java`) are authoritative over the implementation — they define what the system verifiably does.
3. Implementation (production code) is subordinate to both.

Consequences:
- Spec contradicts code → code is wrong.
- Spec contradicts test → test is wrong.
- Test contradicts code → code is wrong.
- Never adjust specs to match passing tests or existing code.
- Never infer requirements from the implementation.

---

## Step 1 – Understand the standard

Read `_template_use-case.md` to confirm the expected structure and quality bar.

---

## Step 2 – Read all use cases

Read every `UC-*.md` file in `specs/`. Build a mental model of:
- All actors and their names across all UCs
- The ubiquitous language used (German or English? consistent?)
- The overall scope of the system (what does it do?)

---

## Step 3 – Review each use case individually

For each UC, check the following four dimensions:

### 3a – Completeness
- Are all mandatory sections present (Brief Description, Actors, Preconditions,
  Trigger, Description, Postconditions, Acceptance Criteria)?
- Are there placeholder texts or unfilled `[square brackets]` remaining?
- Is `completeness` in the front matter set correctly?

### 3b – Gherkin quality
- Does each UC have at least one happy-path and one error scenario?
- Is the `Given` state verifiable (not vague)?
- Is the `When` a single, atomic actor action?
- Is the `Then` an observable, testable result?
- Is the ubiquitous language consistent with the rest of the UC and other UCs?

### 3c – Cross-UC consistency
- Are actor names identical across all UCs? (e.g. not "Admin" in one and
  "Administrator" in another)
- Is the language consistent throughout (all German or all English — no mixing)?
- Do alternative flows and error scenarios reference correct step numbers?
- Are dependencies between UCs reflected (if UC-005 requires UC-002 to have run first,
  is that a Precondition)?

### 3d – Technical feasibility
- Are the main flow steps concrete and implementable, or are they vague hand-waving?
- Are there steps that assume functionality that contradicts other UCs?
- Are there preconditions or postconditions that are technically impossible to verify?
- Are there implicit system integrations or data requirements not yet captured?

---

## Step 4 – Apply fixes

### Minor fixes — apply directly and silently:
- Typos and grammar errors
- Wrong `completeness` value in front matter
- Inconsistent actor names (standardise to the most frequently used form)
- Missing full stop at end of Brief Description
- Gherkin formatting issues (indentation, keyword casing)
- Stray placeholder text (`[square brackets]`, HTML comments)
- Step numbers that are off after edits

### Major issues — do NOT fix, add as Open Item instead:
Add a line to the `## Open Items` section of the affected UC:
`- [ ] REVIEW: <specific finding and suggested resolution>`

Treat the following as major:
- A missing mandatory section where the content cannot be inferred
- A Gherkin scenario that tests the wrong behaviour
- A main flow step that is technically not feasible as written
- An actor that appears inconsistently and the correct name is unclear
- A precondition that depends on another UC not yet specified
- Any business rule or edge case that requires a stakeholder decision

---

## Step 5 – Update front matter

After fixing, update the `completeness` field in each UC's front matter:
- `Minimum` – one or more Open Items remain (including new REVIEW items)
- `Intermediate` – all sections filled, no Open Items, not yet formally approved
- `Complete` – only if no Open Items exist and the UC needs no further input

---

## Step 6 – Deliver a review report

Output a summary table:

| UC File | Fixes Applied | New Open Items | Completeness | Verdict |
|---|---|---|---|---|
| UC-001_Benutzer-Registrieren.md | 3 | 0 | Intermediate | ✓ Ready |
| UC-002_Konto-Sperren.md | 1 | 2 | Minimum | ⚠ Needs input |
| UC-005_Ticket-Erstellen.md | 0 | 4 | Minimum | ✗ Major gaps |

Verdict legend:
- `✓ Ready` – no Open Items, UC is solid
- `⚠ Needs input` – Open Items exist but UC is structurally sound
- `✗ Major gaps` – fundamental issues that block implementation

Below the table, list cross-cutting findings that affect multiple UCs:
- Actor name inconsistencies found and how they were resolved
- Language mixing (German/English) if detected
- Missing dependencies between UCs
- Systemic quality issues to address before the next refinement round

Keep findings crisp — one bullet per finding, no prose paragraphs.