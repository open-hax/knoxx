# Epistemic Kernel

Moved on 2026-09-30. The epistemic model it describes (facts, observations, inferences,
attestations and judgments, with OpenPlanner as the store of record) spans several
Foresight systems, so it now lives in the Foresight monorepo:
[`docs/architecture/epistemic-kernel.md`](https://github.com/open-hax/foresight/blob/main/docs/architecture/epistemic-kernel.md)
(examples: [`epistemic-examples.edn`](https://github.com/open-hax/foresight/blob/main/docs/architecture/epistemic-examples.edn)).

Knoxx's own implementation is `ingestion/src/kms_ingestion/epistemic.cljc`, plus the
`openplanner.*` epistemic tools described in [`mcp-local-testing.md`](mcp-local-testing.md).
