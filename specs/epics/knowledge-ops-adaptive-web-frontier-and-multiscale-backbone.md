# Knowledge Ops — Adaptive Web Frontier + Multiscale Backbone Spec

## Status
Draft

## Current canonical reading

- canonical lake backend: `orgs/open-hax/openplanner`
- canonical web producer: `orgs/octave-commons/myrmex`
- traversal brain: `orgs/octave-commons/graph-weaver-aco`
- fetch/render/extraction backend: `orgs/shuv/shuvcrawl`
- primary graph workbench: `orgs/octave-commons/graph-weaver`
- canonical lake model: `knowledge-ops-source-lakes-cross-lake-graph.md`
- implementation companion specs:
  - `orgs/octave-commons/myrmex/specs/adaptive-frontier-salience-and-template-aware-pruning.md`
  - `orgs/open-hax/openplanner/specs/openplanner-web-edge-salience-and-backbone-projections.md`

This spec extends the canonical `web` lake model with a new rule:

> Observe richly. Decide selectively. Preserve reversibly.

---

## Purpose

Define a state-of-the-art web crawling and graph-retention architecture for Knoxx/OpenPlanner/Myrmex that:

1. preserves raw web graph evidence,
2. expands the frontier intelligently rather than by naive fanout,
3. simplifies graph views without destroying discovery bridges,
4. uses research-backed methods for template detection, crawl prioritization, and backbone extraction,
5. keeps failure semantics explicit and reversible.

---

## Problem statement

The current crawler has three coupled failure modes:

1. **Template fanout dominates discovery**
   - giant listing pages, nav bars, share widgets, app-store pages, legal pages, and account surfaces create huge low-value edge bursts.

2. **Frontier value is not distinguished from graph storage value**
   - an edge that is not worth following immediately may still be important as evidence or as a structural bridge.

3. **Graph reduction risks becoming naive thresholding**
   - global cutoffs over outdegree, weight, or count destroy multiscale structure and belittle small but meaningful nodes.

The system therefore needs separate contracts for:

- what was observed,
- what should be followed next,
- what should be shown by default.

---

## Design thesis

The crawler should behave like a layered scientific instrument:

1. **Observation layer** — store richly structured crawl receipts.
2. **Interpretation layer** — infer block roles, template recurrence, productivity, and salience.
3. **Decision layer** — choose fetch candidates using yield, novelty, diversity, and exploration.
4. **Backbone layer** — derive sparse graph views that preserve important local and bridge structure.

No single threshold or score should be allowed to destructively govern all four layers.

---

## Research basis

This spec synthesizes several research directions:

### 1. Focused crawling / link-context scoring

- Chakrabarti et al., focused crawling
- context-graph focused crawling
- Tang et al., focused crawling for topical relevance and quality
- Dang et al., "Look back, look around" (LBLA)

Main takeaways:
- link context matters,
- recent productivity matters,
- nearby productive pages matter,
- frontier ranking should optimize expected useful discovery rather than raw graph growth.

### 2. Online importance / adaptive ranking

- OPIC / adaptive online page importance computation

Main takeaways:
- page importance can be updated online,
- changing graphs need decayed/adaptive history,
- external relevance can be fused into link-only importance.

### 3. Boilerplate / template removal

- Boilerpipe
- CETR
- Web2Text
- visual-feature main-content extraction
- hyperlink/template analysis work

Main takeaways:
- DOM/text alone is insufficient,
- repeated site blocks are strong template signals,
- visual position and structural recurrence are useful features,
- navigation/template pruning should happen at the block level, not only at the URL level.

### 4. Statistical graph backbone extraction

- disparity filter / local multiscale backbone extraction
- noise-corrected backbone extraction
- comparative graph sparsification literature

Main takeaways:
- global thresholds are poor multiscale reducers,
- local significance is more faithful than global weight cuts,
- different sparsifiers preserve different properties,
- there should be more than one derived sparse view.

### 5. Exploration/exploitation control

- RL / epsilon-greedy focused crawling literature

Main takeaways:
- exploit-only crawlers self-trap,
- explicit exploration budget is necessary,
- exploration should decay but never hit zero.

---

## Novel synthesis

This spec does **not** claim as a fact that no one has ever built any similar individual component.
What it does claim is that Knoxx can implement a novel synthesis for this stack by combining:

1. **template-memory over repeated DOM/link blocks**,
2. **LBLA-style new-outlink productivity prediction**,
3. **OPIC-style adaptive page importance**,
4. **ACO traversal with host-balance and reversible backpressure pauses**,
5. **epsilon exploration floors**,
6. **multiscale statistical backbones**,
7. **bridge rescue for rare cross-host / cross-topic / cross-lake structure**,
8. **OpenPlanner-backed reversible raw-vs-derived graph views**.

This combination is the intended cutting edge.

---

## Goals

1. Preserve raw web graph receipts in the canonical `web` lake.
2. Distinguish main-content links from template/nav/share/action links.
3. Prioritize edges that maximize expected discovery yield.
4. Maintain host and topic diversity under high-fanout domains.
5. Allow graph compression without deleting evidentiary structure.
6. Preserve rare bridge edges even when they are not globally heavy.
7. Expose multiple derived graph views for different tasks.
8. Keep the entire system explainable and queryable.

---

## Non-goals

1. Proving perfect global originality.
2. Hard-deleting raw observations once ingested.
3. Replacing OpenPlanner with a specialized graph DB.
4. Solving general web relevance for every domain at once.
5. Requiring an ML model before a useful first implementation exists.

---

## Canonical layers

## Layer 0 — Raw observation

Canonical truth remains append-only web receipts:

- page visits
- discovered links
- fetch metadata
- extraction metadata
- source page provenance
- discovery channel (`html`, `feed`, `sitemap`, `manual`, future `social`)

Invariant:

> Raw observation is preserved even when derived decision/view layers reject the edge.

## Layer 1 — Block understanding

For each observed outgoing link, the system should infer or later derive:

- block signature
- block role
- template recurrence
- anchor/context features
- visual/DOM position features when available

This is the first non-naive pruning layer.

## Layer 2 — Frontier salience

Every candidate edge gets a salience estimate for crawl expansion.

This is not a delete flag.
It is a fetch-priority estimate.

## Layer 3 — Frontier governor

The frontier governor decides what to follow next using:

- source productivity,
- target novelty,
- host diversity,
- template penalties,
- exploration budget,
- backpressure headroom.

## Layer 4 — Backbone derivation

Sparse graph views are derived from aggregated edge evidence.
These are task-specific and reversible.

## Layer 5 — User/query surfaces

Knoxx and Graph Weaver should surface:

- raw graph
- discovery backbone
- structural backbone
- evidence backbone
- bridge backbone

with explicit labels so users know what they are seeing.

---

## Core data abstractions

## Observed edge receipt

A raw edge observation should be representable as:

```json
{
  "source_url": "https://example.com/source",
  "target_url": "https://example.com/target",
  "discovery_channel": "html",
  "anchor_text": "related article",
  "anchor_context": "surrounding text snippet",
  "dom_path": "body/main/article/aside/ul/li/a",
  "block_signature": "host:example.com:block:abc123",
  "block_role": "main_content | nav | footer | share | auth | social | promo | legal | catalog | unknown",
  "source_fetch_ts": "2026-04-04T00:00:00Z",
  "source_node_id": "web:url:https://example.com/source",
  "target_node_id": "web:url:https://example.com/target"
}
```

## Host block memory

For each `(host, block_signature)` maintain:

- pages_seen_with_block
- fraction_of_host_pages_with_block
- repeated_target_host_distribution
- repeated_target_path_prefix_distribution
- historical_block_yield
- inferred_block_role confidence

## Page productivity memory

For each source page maintain rolling features:

- visits
- new_targets_first_seen
- retained_targets_after_filter
- downstream_useful_targets
- recent_new_outlink_rate
- nearby_page_new_outlink_rate
- OPIC-style importance / cash-history score

## Derived edge salience

Each `(source,target)` edge receives a derived salience record with:

- content_block_score
- template_repeat_penalty
- source_productivity_score
- neighborhood_productivity_score
- target_novelty_score
- host_diversity_bonus
- target_class_bonus
- action_or_static_penalty
- exploration_override
- final_follow_score

## Backbone membership

Each aggregated edge may be tagged into zero or more derived backbones:

- `discovery`
- `structural`
- `evidence`
- `bridge`
- future `semantic`

---

## Edge scoring model

## First-order salience formula

```text
follow_score(e) =
  + a1 * source_productivity(e.source)
  + a2 * neighborhood_productivity(e.source)
  + a3 * main_content_score(e)
  + a4 * target_novelty(e.target)
  + a5 * target_class_bonus(e.target)
  + a6 * host_diversity_bonus(e.target_host)
  - b1 * template_repeat_penalty(e)
  - b2 * action_share_static_penalty(e.target)
  - b3 * cross_host_noise_penalty(e)
```

The score may be implemented as additive, multiplicative, or logistic.
The invariant is more important than the first exact formula:

> content and productivity should raise score; repeated template behavior and action/static behavior should lower it.

## Required feature families

### Positive signals

- source page recently produced useful new targets
- source neighborhood recently produced useful new targets
- edge originates in likely main-content block
- target host/path is novel in recent window
- target class is likely valuable:
  - issue
  - PR
  - release
  - changelog
  - advisory
  - paper
  - documentation
  - feed entry
  - standards/spec page

### Negative signals

- block repeats across a high fraction of host pages
- target belongs to action/share/auth/session/app-store/legal/static asset flows
- same target host/path family has already dominated recent crawl budget
- target is a known chrome/service endpoint with low historical yield

---

## Template-memory contract

Template detection must not rely solely on brittle URL regexes.

The system should accumulate a host-local memory of repeated link blocks.
A block should be strongly penalized when:

1. a similar block signature appears across many pages on the same host,
2. it emits highly similar target sets,
3. it lives in a visually/structurally chrome-like region,
4. its historical yield is low.

### Version 1 block-role classifier

A pragmatic first release may infer roles using:

- DOM depth and path tokens
- CSS/id/class tokens
- top/bottom/sidebar/center placement
- number of links in the block
- anchor text repetitiveness
- target-host concentration
- recurrence across sampled host pages

Expected roles:

- `main_content`
- `related_content`
- `nav`
- `footer`
- `share`
- `social`
- `auth`
- `app_store`
- `legal`
- `promo`
- `catalog`
- `unknown`

---

## Frontier-governor contract

The governor should choose next fetches by combining:

1. **page importance** — OPIC-like adaptive score,
2. **expected new-outlink yield** — LBLA-like recent productivity,
3. **edge salience** — content/template/context-aware edge score,
4. **host budget** — limit monopolization,
5. **exploration** — epsilon floor,
6. **backpressure headroom** — downstream queue-aware gating.

### Frontier retention rule

For each page, do not send all outgoing links into equal competition.
Instead:

- keep all mandatory-class edges,
- keep top `K_page` by `follow_score`,
- keep top `K_block` within each block,
- keep first-discovery bridges to new hosts/path families,
- sample `ε` from the remainder.

This ensures:

- giant pages cannot dominate purely by fanout,
- small but valuable blocks survive,
- exploration never vanishes.

---

## Multiscale backbone family

There is no single best sparse graph.
The system should materialize multiple derived backbones.

## 1. Discovery backbone

Optimized for:
- expected discovery yield,
- recent productivity,
- novelty,
- high-salience expansion paths.

Use:
- frontier analysis,
- agentic browsing,
- watchlist expansion.

## 2. Structural backbone

Optimized for:
- template-corrected site topology,
- host/path hierarchy,
- stable route structure.

Use:
- graph visualization,
- site understanding,
- detecting repeated structure and subsites.

## 3. Evidence backbone

Optimized for:
- preserving high-confidence observed relationships,
- temporal persistence,
- repeated corroboration.

Use:
- provenance,
- auditability,
- memory traces.

## 4. Bridge backbone

Optimized for:
- rare cross-host, cross-topic, or cross-lake connectors.

Use:
- exploration,
- novelty discovery,
- preventing over-pruning of weak but strategic ties.

### Bridge rescue rule

After local significance filtering, rescue edges that are any of:

- first or only edge from a productive source into a new host cluster,
- only surviving connector between two discovered communities,
- high semantic-distance but high-yield connector,
- cross-lake reference bridge (`web -> devel`, future `web -> bluesky`).

This is a key novelty of the design.

---

## Backbone extraction rule

The default statistical reducer should be local-significance based, not global-threshold based.

Candidate methods:

- disparity filter,
- noise-corrected backbone,
- hybrid local significance with bridge rescue.

### Backbone keep criterion

An aggregated edge may survive if any of the following hold:

1. locally significant for the source,
2. locally significant for the target,
3. top-ranked outgoing edge by local salience,
4. persistent over time,
5. mandatory semantic class,
6. rescued as a bridge.

---

## OpenPlanner contract additions

OpenPlanner should become the reversible memory surface for:

- raw web edge receipts,
- derived edge features,
- page productivity state,
- host block template state,
- backbone memberships.

Derived state may live in projection tables/materialized views rather than in the append-only raw stream itself.

Invariant:

> Raw receipt truth is append-only. Derived frontier/backbone state is recomputable.

---

## Graph Weaver / Knoxx surface contract

Graph surfaces should default to a declared derived view, not silently mix raw and sparse edges.

Required user-selectable modes:

- `raw-web-graph`
- `discovery-backbone`
- `structural-backbone`
- `evidence-backbone`
- `bridge-backbone`

Required explanations:

- what the current view keeps,
- what it prunes,
- which score/significance method produced it,
- whether bridge rescue is enabled.

---

## Evaluation metrics

## Crawl metrics

- useful new pages per 100 fetches
- unique host discovery rate
- unique path-family discovery rate
- host concentration / dominance index
- frontier entropy
- time-to-first-novel-host
- time-to-first-useful-page

## Graph metrics

- raw edge count vs backbone edge count
- node retention
- bridge retention
- community preservation
- centrality-rank drift
- edge-role composition before/after pruning

## System metrics

- backpressure pause frequency
- mean pause duration
- queue overshoot above limit
- Shuvcrawl health stability
- OpenPlanner write latency

---

## Phases

### Phase 1 — Receipt enrichment

- attach edge/block/context metadata to observed links
- add host block memory
- explicitly classify obvious low-value roles

### Phase 2 — Frontier salience

- implement derived edge salience records
- add source/neighborhood productivity features
- add target class bonuses and template penalties

### Phase 3 — Adaptive governor

- couple ACO scheduling to salience + productivity
- add exploration floor
- add host/path-family budgets
- expose decision receipts

### Phase 4 — Backbone projections

- aggregate edges over time
- compute local significance
- materialize discovery / structural / evidence / bridge backbones

### Phase 5 — Query and UI

- add query surfaces in OpenPlanner / Knoxx / Graph Weaver
- let users switch among backbones
- show explanation metadata in UI

---

## Verification

1. High-fanout template pages no longer dominate follow decisions.
2. Raw observation remains queryable after derived pruning.
3. Backbones preserve more nodes/bridges than naive thresholding at comparable edge counts.
4. Host diversity improves without collapsing useful discovery yield.
5. Pause/resume remains stable under lake backpressure.
6. Default graph view becomes explainable and switchable.

---

## Definition of done

This spec is implemented when Knoxx can truthfully say:

- we do not naively prune edges,
- we retain raw web evidence,
- we score frontier edges using template/context/productivity signals,
- we preserve multiple sparse backbones for different tasks,
- we can explain why a link was followed, downweighted, or hidden.
