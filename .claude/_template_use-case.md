---
id: UC-XXX
type: Use Case
name: "[Use Case Name]"
completeness: Minimum  # Minimum | Intermediate | Complete
---

# UC-XXX – [Use Case Name]

<!--
  INSTRUCTIONS
  - Replace all placeholders in [square brackets] and in the front matter.
  - UC-ID is product-scoped unique and immutable – never change, even on deprecation.
  - Register the Link Service ID before creating the file and enter it above.
  - Remove all comment blocks before committing.
-->

---

## Brief Description

> [One sentence: "[Actor] wants to [goal] in order to [benefit]."]

---

## Actors

| Actor | Type | Role |
|---|---|---|
| [Primary Actor] | `Human` \| `System` \| `Time` | [Short role description] |
| [Secondary Actor] | `Human` \| `System` | [Short role description] *(remove row if not applicable)* |

---

## Context & Background

> [2–4 sentences: What does one need to know about the functional and technical context to understand this use case without prior knowledge? This section is especially important for AI-assisted processing and for new team members.]

---

## Preconditions

- [Precondition – formulated in a verifiable way]

---

## Trigger

> [What initiates this use case? E.g.: "User opens the configuration page", "Pipeline job is started", "Daily scheduled job at 03:00"]

---

## Description

1. Step one
2. Step two
3. Step three

---

## Alternative Flows

### A1 – [Name]

> Entry point: step [N] of the main flow

1. A1.1: ...
2. A1.2: ...
3. A1.3: ...

---

## Error Scenarios

### E1 – [Error Name]

> Entry point: step [N] of the main flow

1. E1.1: Error message
2. E1.2: Possible correction or abort

---

## Postconditions

### Success

- [What holds after successful completion – formulated in a verifiable way]

### Failure / Abort

- [What holds after abort? Rollback, logs, notifications?]

---

## Acceptance Criteria

```gherkin
Scenario: [Normal scenario]
  Given [initial state]
  When  [actor's action]
  Then  [expected result]

Scenario: [Error scenario]
  Given [initial state]
  When  [invalid or erroneous action]
  Then  [expected system reaction]
```

---

## Non-Functional Requirements

<!-- Only fill in if specific to this use case. Otherwise remove section. -->

| Type | Requirement |
|---|---|
| Performance | [e.g. response time < 2s] |
| Availability | [e.g. 24/7 accessible] |
| Security | [e.g. authentication via SSO required] |

---

## Dependencies & References

<!-- Only fill in if cross-references exist. Otherwise remove section. -->

- **Included Use Cases**: [Link Service links to included use cases]
- **Depends on**: [Link Service links to prerequisite use cases or artifacts]

---

## Open Items

<!-- All items must be resolved before changing status to 'approved'. Remove section after resolution. -->

- [ ] [Open question or pending clarification]