---
category: "tasks"
labels: ["tasks", "has-parent", "translation", "publication", "agents", "contracts", "wave-2"]
points: "5"
title: "Translation — compose the agent actor that produces translations"
priority: "P1"
status: "review"
uuid: "knoxx-translation-agent-actor-composition"
created_at: "2026-08-26T00:00:00Z"
---

# Translation — compose the agent actor that produces translations

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

Give the publication gate a producer that this deployment actually contains.

`knoxx-translation-work-dispatch` connects derived work to the OpenPlanner
ingestion worker, and `knowledge-ops-translation-mt-pipeline` shows that worker
lives in `ingestion/`. The production compose stack for `open-hax.promethean.rest`
builds `proxx`, `knoxx` and `caddy` and nothing else — so every dispatch queued a
batch nothing would pick up, and the four localized intents stayed blocked with
no surface reporting why.

The producer is an **agent actor**, composed from contracts that already existed.
This card explicitly does not introduce a worker, a second evidence model, or a
second identity scheme.

## What already existed and was never connected

| Contract | Supplies | Before |
|---|---|---|
| `contracts/roles/translator.edn` | the role, carrying `:cap/translation` | existed |
| `contracts/capabilities/cap_translation.edn` | the tool, `save_translation` | existed |
| `docs/design/resource-architecture.md` runtime | event → trigger → `:actions/start-agent-session` | implemented 2026-06-10 |
| `contracts/agents/publication_translator.edn` | an agent holding that role | **missing** |
| `contracts/namespaces/publication.edn` | the trigger that runs it | **missing** |

One hop was also missing in code: `:actions/start-agent-session` never forwarded
resource policies, so a session could not be pinned to the claim it ran for.

## Work

- Declare the translator agent and the trigger that runs it, in both this repo's
  contract set and the deployed one in `open-hax/services`.
- Forward a resource-policy overlay from the event to the started session, only
  for a trigger that opts in with `:resource-policies-from-event`.
- Give `save_translation` a contract-backed sink, selected by the session's own
  pin rather than by configuration, that records evidence through the existing
  completion path.
- Store the submitted bytes keyed by the produced *output* revision, and read
  them back only through the receipt that names it.
- Select the producer per deployment, defaulting to the agent.

## Definition of Done

- A gated translation work item starts a pinned agent session, and the agent's
  submission becomes a revision-specific receipt the publication gate recognizes.
- A submission naming a different document, garden, locale or organization than
  the session was pinned to is refused, with a message the agent can act on.
- Re-translating one source revision produces a different output revision, so an
  approval of the first cannot authorize the second.
- Asking twice starts one session.
- The deploy verifier fails when nothing in the stack can produce a translation.

## Card premise corrections

This card was written after the code, from a correction to a session that had
gone the other way, so its premises are unusually accurate. Two things still
turned out differently than the obvious reading.

1. **"The run id is the agent session id."** That was the first design and it is
   wrong. `domain.action.start-agent-session` derives session and conversation
   ids from the trigger and the event; taking that over would couple this
   composition to identifiers whose format is the runtime's business. The run id
   travels in the session's resource policies instead, so the two schemes stay
   independent — see `law.translation-agent/run-id`.

   It is derived from the dispatch key **and the claim instant**, not the key
   alone. The key is deliberately stable across attempts, which is what makes a
   duplicate a duplicate; a run id derived from it would be identical on
   re-translation, the output revision would not change, and an approval of the
   first translation would silently authorize the second.

2. **"The pin lives in the trigger contract."** A trigger contract is one EDN
   value reused for every document and locale, while the pin is per-attempt.
   Written into `:trigger/with` it could only ever name one document, so either
   every locale of every document needed its own trigger, or a generic action had
   to learn to infer a translation pin from a payload. The overlay therefore
   travels on the event and the trigger states in one field that it accepts one.

## What this card strengthens beyond its own scope

`knoxx-translation-work-dispatch` records that `source-drift-refusal` "cannot see
what the worker read": the worker is handed a document id and fetches its own
input, so comparing repository digests before and after never establishes what
was translated.

The brief embeds the source document verbatim, so an agent translates the
dispatched revision by construction and the drift check becomes a redundant
second one. The bound this costs is `law.translation-agent/max-brief-chars`: an
oversize document is refused at emit time rather than truncated, because a
truncated source would be translated in full by an agent with no way to know it
read a fragment, and the receipt would attest that the whole revision was done.

## Known gaps left open

### A dead session strands its claim

`infra.translation-dispatch/recover-settled-batch!` can re-read a batch and learn
that a document finished even when the completion bookkeeping was lost. An agent
session has no equivalent read here, so a claim whose session dies mid-run stays
in flight: the gate keeps reporting the translation missing while every later
pass answers duplicate, and that revision needs an operator.

Recorded as data on `infra.translation-agent-dispatch/known-gap` and printed as a
`WARN` by both verification scripts every run. Closing it needs a session-state
read the dispatch layer does not currently have, and that is its own card.

### Segmented documents are refused, not reassembled

A contract-backed document's unit of content is a file, and the digest the whole
publication chain keys on is a digest of that file. Accepting numbered segments
would require reassembling them, and concatenation order is not guaranteed to
reproduce the file — the open design question already recorded on
`knoxx-translation-work-dispatch`. So a submission with a nonzero
`segment_index` is refused with instructions rather than silently joined. The
OpenPlanner segment path is untouched and still segments.

**Superseded for the restored workflow:** `knoxx-translation-split-memory-feedback` makes the
split manifest and its ordered members part of the pre-provider claim, so order and exact
composition are no longer guessed from independent tool calls. The current whole-file-only
`segment_index 0` behavior remains historical implementation evidence, not the target contract.

### The authored locale fallback is still shipped

`infra.publication-contract-content` predates any producer and turns authored
locale files into the same evidence. It was kept deliberately, so every declared
locale stays renderable while the agent path fills in; its `authored-at` is fixed
at the epoch so agent output always supersedes it. Retiring it is a decision
about whether hand-authored translations are a supported input, not a cleanup.
