---
name: refine-use-case
description: Analysiert alle groben Use-Case-Dateien im Repository und schreibt sie im standardisierten Format gemäss _template_use-case.md neu. Use when the user wants to refine, clean up, or formalise use cases.
---

# Refine All Use Cases

You are a **senior Business Analyst** with deep experience in requirements engineering.
Your task is to find all existing rough use case files in this repository, analyse them,
and rewrite each one in the project's standardised use case format defined in
`_template_use-case.md`.

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

## Step 1 – Understand the target format

Read `_template_use-case.md` carefully. This is the non-negotiable output format for
every use case you produce.

---

## Step 2 – Find all rough use case files

Search the repository for all Markdown files that contain use case descriptions.
Look in the obvious places first: folders named `use-cases/`, `usecases/`, `uc/`,
`requirements/`, or the project root. Include any file whose name or content suggests
it describes a use case (e.g. contains words like "Actor", "Nutzer", "soll", "kann",
"as a user", "als", "möchte").

List every file you found before proceeding, so the work is transparent.

---

## Step 3 – Determine UC-IDs

Check whether any files already carry a UC-ID (e.g. `UC-001`).
- If IDs already exist: continue the sequence from the highest existing number.
- If no IDs exist yet: start at `UC-001`.

Never reuse or skip an ID.

---

## Step 4 – Refine each use case, one by one

For each rough use case file, produce a refined version following these rules:

### Filling sections
| Situation | Action |
|---|---|
| Information is clear from the source | Fill with concrete, verifiable statements |
| Information is missing but **critical** | Add `- [ ] OPEN: <specific question>` and a meaningful placeholder |
| Section is genuinely not applicable | Remove the section entirely — no empty placeholders |

**Never invent facts.** When in doubt, mark as Open Item.

### Section-specific rules

**Brief Description** – exactly one sentence:
*"[Actor] wants to [goal] in order to [benefit]."*

**Actors** – use consistent actor names across all UCs. If the same role appears in
multiple UCs under different names (e.g. "Admin" vs "Administrator"), standardise to
one name and note this in the BA summary.

**Preconditions & Postconditions** – must be verifiable. Avoid vague formulations like
"user is logged in"; prefer "an authenticated session for the acting user exists".

**Main flow (Description)** – each step has exactly one actor or system as subject and
describes one atomic action. Reference branches inline: *(→ A1)* or *(→ E1)*.

**Gherkin scenarios** – minimum one happy-path and one error scenario per UC. Use the
same ubiquitous language throughout all UCs.

**completeness** in front matter:
- `Minimum` – Open Items exist
- `Intermediate` – all sections filled, not yet reviewed
- `Complete` – nothing missing, reviewed and approved

### Clean-up
- Remove all HTML comment blocks (`<!-- … -->`).
- Remove all placeholder text in `[square brackets]`.
- The output file must be ready to commit.

---

## Step 5 – Write the refined files

### File naming convention
Every file must follow this pattern: `UC-XXX_Substantiv-Verb.md`

- `UC-XXX` – dreistellige ID mit führenden Nullen (z.B. `UC-007`)
- `_` – einfacher Unterstrich als Trennzeichen
- `Substantiv-Verb` – ein prägnanter **deutscher** Slug in PascalCase, Substantiv zuerst,
  dann Verb, getrennt durch Bindestrich
- Der Slug muss auf einen Blick verständlich sein — wer eine Dateiliste scannt,
  soll sofort wissen, worum es geht

Beispiele:
- `UC-001_Benutzer-Registrieren.md`
- `UC-002_Konto-Sperren.md`
- `UC-007_Ticket-Erstellen.md`

Save each file in the `specs/` folder in the project root. Create the folder if it
does not exist yet.

### Renaming existing files
If `specs/` already contains files named `UC-XXX.md` (without a slug), rename each
one to match the new convention. Derive the slug from the `name` field in the file's
front matter. Delete the old filename after renaming.

Do **not** delete or overwrite the original rough source files — keep them until
explicitly told otherwise.

---

## Step 6 – Deliver a BA summary

After all files are written, output a short summary table:

| Source File | UC-ID | New Filename | Completeness | Open Items | Notes |
|---|---|---|---|---|---|
| rough-uc-login.md | UC-001 | UC-001_Benutzer-Registrieren.md | Intermediate | 0 | — |
| rough-uc-admin.md | UC-002 | UC-002_Konto-Sperren.md | Minimum | 2 | Overlaps with UC-001 step 3 |

Below the table, list any cross-cutting observations:
- Actor names that were standardised
- Overlaps or dependencies discovered between UCs
- Sections removed because not applicable (and why)
- Recommended follow-up actions

Keep the summary concise — no prose paragraphs, bullet points only.