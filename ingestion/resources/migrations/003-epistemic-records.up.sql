-- Epistemic records: facts, observations, inferences, attestations, judgments,
-- and actor-bindings ingested from promptdb EDN files or emitted by the
-- contract runtime.
--
-- :kind is the discriminator that maps 1-to-1 with the epistemic schema kinds
-- defined in kms-ingestion.epistemic.
-- :payload is the full EDN record stored as JSONB so we never lose structure.
-- :org_id + :actor_id carry provenance for agent-emitted records; both are
-- NULL for records ingested straight from promptdb files.
-- :causedby links an attestation/inference back to the triggering run-id.
-- :p is the confidence value [0,1].

CREATE TABLE IF NOT EXISTS epistemic_records (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  kind        TEXT        NOT NULL CHECK (kind IN ('fact','obs','inference','attestation','judgment','actor-binding')),
  tenant_id   TEXT        NOT NULL REFERENCES tenants(tenant_id),
  org_id      TEXT,
  actor_id    TEXT,
  contract_id TEXT,
  causedby    UUID,
  p           NUMERIC(4,3) CHECK (p >= 0 AND p <= 1),
  payload     JSONB       NOT NULL,
  src         TEXT,
  asserted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_epistemic_kind      ON epistemic_records(kind);
CREATE INDEX IF NOT EXISTS idx_epistemic_tenant    ON epistemic_records(tenant_id);
CREATE INDEX IF NOT EXISTS idx_epistemic_actor     ON epistemic_records(actor_id);
CREATE INDEX IF NOT EXISTS idx_epistemic_contract  ON epistemic_records(contract_id);
CREATE INDEX IF NOT EXISTS idx_epistemic_causedby  ON epistemic_records(causedby);
CREATE INDEX IF NOT EXISTS idx_epistemic_payload   ON epistemic_records USING GIN (payload);
