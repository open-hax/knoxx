(ns knoxx.backend.infra.db.policy.schema
  "DDL migration and seed data. These are the only raw SQL strings in the
   policy DB layer — HoneySQL does not support CREATE TABLE / ALTER TABLE."
  (:require [knoxx.backend.extern.pg :as pg]
            [knoxx.backend.infra.registry.tools :as tool-registry]))

(def ^:private schema-ddl
  "
    CREATE EXTENSION IF NOT EXISTS pgcrypto;

    CREATE TABLE IF NOT EXISTS orgs (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      slug TEXT NOT NULL UNIQUE,
      name TEXT NOT NULL,
      kind TEXT NOT NULL DEFAULT 'customer',
      is_primary BOOLEAN NOT NULL DEFAULT FALSE,
      status TEXT NOT NULL DEFAULT 'active',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS users (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      email TEXT NOT NULL UNIQUE,
      display_name TEXT NOT NULL,
      auth_provider TEXT NOT NULL DEFAULT 'bootstrap',
      external_subject TEXT,
      status TEXT NOT NULL DEFAULT 'active',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS memberships (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      actor_id TEXT,
      status TEXT NOT NULL DEFAULT 'active',
      is_default BOOLEAN NOT NULL DEFAULT FALSE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE (user_id, org_id)
    );

    CREATE INDEX IF NOT EXISTS idx_memberships_user_id ON memberships (user_id);
    CREATE INDEX IF NOT EXISTS idx_memberships_org_id ON memberships (org_id);
    CREATE INDEX IF NOT EXISTS idx_memberships_actor_id ON memberships (actor_id);

    CREATE TABLE IF NOT EXISTS roles (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      org_id UUID REFERENCES orgs(id) ON DELETE CASCADE,
      name TEXT NOT NULL,
      slug TEXT NOT NULL,
      scope_kind TEXT NOT NULL DEFAULT 'org',
      built_in BOOLEAN NOT NULL DEFAULT FALSE,
      system_managed BOOLEAN NOT NULL DEFAULT FALSE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      CHECK (scope_kind IN ('platform', 'org'))
    );

    CREATE UNIQUE INDEX IF NOT EXISTS roles_platform_slug_uniq
      ON roles (slug) WHERE org_id IS NULL;

    CREATE UNIQUE INDEX IF NOT EXISTS roles_org_slug_uniq
      ON roles (org_id, slug) WHERE org_id IS NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_roles_org_id ON roles (org_id);

    CREATE TABLE IF NOT EXISTS role_permissions (
      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
      permission_code TEXT NOT NULL,
      effect TEXT NOT NULL DEFAULT 'allow',
      PRIMARY KEY (role_id, permission_code),
      CHECK (effect IN ('allow', 'deny'))
    );

    ALTER TABLE role_permissions ADD COLUMN IF NOT EXISTS permission_code TEXT;

    DO $$
    BEGIN
      IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'permissions'
      ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'role_permissions'
          AND column_name = 'permission_id'
      ) THEN
        UPDATE role_permissions rp
        SET permission_code = p.code
        FROM permissions p
        WHERE rp.permission_code IS NULL AND rp.permission_id = p.id;
      END IF;
    END $$;

    CREATE UNIQUE INDEX IF NOT EXISTS role_permissions_role_code_uniq
      ON role_permissions (role_id, permission_code)
      WHERE permission_code IS NOT NULL;

    CREATE TABLE IF NOT EXISTS membership_roles (
      membership_id UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
      PRIMARY KEY (membership_id, role_id)
    );

    CREATE TABLE IF NOT EXISTS tool_definitions (
      id TEXT PRIMARY KEY,
      label TEXT NOT NULL,
      description TEXT NOT NULL,
      risk_level TEXT NOT NULL DEFAULT 'medium'
    );

    CREATE TABLE IF NOT EXISTS role_tool_policies (
      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
      tool_id TEXT NOT NULL REFERENCES tool_definitions(id) ON DELETE CASCADE,
      effect TEXT NOT NULL DEFAULT 'allow',
      constraints_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      PRIMARY KEY (role_id, tool_id),
      CHECK (effect IN ('allow', 'deny'))
    );

    CREATE TABLE IF NOT EXISTS user_tool_policies (
      membership_id UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
      tool_id TEXT NOT NULL REFERENCES tool_definitions(id) ON DELETE CASCADE,
      effect TEXT NOT NULL DEFAULT 'allow',
      constraints_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      PRIMARY KEY (membership_id, tool_id),
      CHECK (effect IN ('allow', 'deny'))
    );

    CREATE TABLE IF NOT EXISTS actor_credentials (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      provider TEXT NOT NULL,
      kind TEXT NOT NULL DEFAULT 'credential',
      account_identifier TEXT,
      secret_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      status TEXT NOT NULL DEFAULT 'active',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE (user_id, org_id, provider, kind)
    );

    CREATE INDEX IF NOT EXISTS actor_credentials_user_org_idx
      ON actor_credentials (user_id, org_id, provider);

    CREATE TABLE IF NOT EXISTS data_lakes (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      name TEXT NOT NULL,
      slug TEXT NOT NULL,
      kind TEXT NOT NULL DEFAULT 'workspace_docs',
      config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      status TEXT NOT NULL DEFAULT 'active',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE (org_id, slug)
    );

    CREATE INDEX IF NOT EXISTS idx_data_lakes_org_id ON data_lakes (org_id);

    CREATE TABLE IF NOT EXISTS audit_events (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
      actor_membership_id UUID REFERENCES memberships(id) ON DELETE SET NULL,
      org_id UUID REFERENCES orgs(id) ON DELETE SET NULL,
      action TEXT NOT NULL,
      resource_kind TEXT NOT NULL,
      resource_id TEXT,
      before_json JSONB,
      after_json JSONB,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE INDEX IF NOT EXISTS idx_audit_events_org_created ON audit_events (org_id, created_at);
    CREATE INDEX IF NOT EXISTS idx_audit_events_action_resource ON audit_events (action, resource_kind, created_at);

    CREATE TABLE IF NOT EXISTS sessions (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      membership_id UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      token_hash TEXT NOT NULL,
      token_prefix TEXT NOT NULL DEFAULT '',
      salt TEXT NOT NULL,
      email TEXT NOT NULL,
      display_name TEXT NOT NULL,
      auth_provider TEXT NOT NULL DEFAULT 'github',
      external_subject TEXT,
      ip_address TEXT,
      user_agent TEXT,
      expires_at TIMESTAMPTZ NOT NULL,
      last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    ALTER TABLE sessions ADD COLUMN IF NOT EXISTS token_prefix TEXT NOT NULL DEFAULT '';
    ALTER TABLE memberships ADD COLUMN IF NOT EXISTS actor_id TEXT;

    CREATE INDEX IF NOT EXISTS sessions_user_idx ON sessions (user_id);
    CREATE INDEX IF NOT EXISTS sessions_membership_idx ON sessions (membership_id);
    CREATE INDEX IF NOT EXISTS sessions_token_prefix_idx ON sessions (token_prefix);
    CREATE INDEX IF NOT EXISTS sessions_expires_at_idx ON sessions (expires_at);

    CREATE TABLE IF NOT EXISTS invites (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      code TEXT NOT NULL UNIQUE,
      email TEXT NOT NULL,
      inviter_membership_id UUID REFERENCES memberships(id) ON DELETE SET NULL,
      role_slugs JSONB NOT NULL DEFAULT '[]'::jsonb,
      status TEXT NOT NULL DEFAULT 'pending',
      redeemed_by UUID REFERENCES users(id) ON DELETE SET NULL,
      redeemed_at TIMESTAMPTZ,
      expires_at TIMESTAMPTZ NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE INDEX IF NOT EXISTS invites_org_idx ON invites (org_id);
    CREATE INDEX IF NOT EXISTS invites_code_idx ON invites (code);
    CREATE INDEX IF NOT EXISTS invites_status_idx ON invites (status);

    CREATE TABLE IF NOT EXISTS knoxx_config (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS actor_mailbox_entries (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      kind TEXT NOT NULL DEFAULT 'actor-message',
      status TEXT NOT NULL DEFAULT 'pending',
      source_actor_id TEXT, source_session_id TEXT, source_conversation_id TEXT,
      source_run_id TEXT, source_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      target_kind TEXT NOT NULL DEFAULT 'unknown',
      target_actor_id TEXT, target_session_id TEXT, target_conversation_id TEXT,
      target_run_id TEXT, target_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      delivery_mode TEXT NOT NULL DEFAULT 'follow-up',
      attempts INT NOT NULL DEFAULT 0,
      next_at TIMESTAMPTZ, expires_at TIMESTAMPTZ, delivered_at TIMESTAMPTZ,
      acknowledged_at TIMESTAMPTZ,
      content_ref_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      preview TEXT, last_error TEXT,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      CHECK (status IN ('pending','delivered','failed','expired','superseded','acknowledged')),
      CHECK (delivery_mode IN ('steer','follow-up','event','inbox-only','direct-run'))
    );

    CREATE INDEX IF NOT EXISTS actor_mailbox_status_next_idx ON actor_mailbox_entries (status, next_at);
    CREATE INDEX IF NOT EXISTS actor_mailbox_target_actor_idx ON actor_mailbox_entries (target_actor_id, status, created_at DESC);
    CREATE INDEX IF NOT EXISTS actor_mailbox_target_session_idx ON actor_mailbox_entries (target_session_id, status, created_at DESC);
    CREATE INDEX IF NOT EXISTS actor_mailbox_source_run_idx ON actor_mailbox_entries (source_run_id, created_at DESC);

    CREATE TABLE IF NOT EXISTS actor_mailbox_routes (
      actor_id TEXT PRIMARY KEY,
      conversation_id TEXT, session_id TEXT, run_id TEXT, contract_id TEXT,
      status TEXT NOT NULL DEFAULT 'active',
      source_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      expires_at TIMESTAMPTZ,
      last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      CHECK (status IN ('active','inactive'))
    );

    CREATE INDEX IF NOT EXISTS actor_mailbox_routes_session_idx ON actor_mailbox_routes (session_id);
    CREATE INDEX IF NOT EXISTS actor_mailbox_routes_conversation_idx ON actor_mailbox_routes (conversation_id);
    CREATE INDEX IF NOT EXISTS actor_mailbox_routes_status_seen_idx ON actor_mailbox_routes (status, last_seen_at DESC);

    CREATE TABLE IF NOT EXISTS studio_state (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      kind TEXT NOT NULL DEFAULT 'player',
      state_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE (user_id, org_id, kind)
    );

    CREATE INDEX IF NOT EXISTS studio_state_user_idx ON studio_state (user_id);
    CREATE INDEX IF NOT EXISTS studio_state_org_idx ON studio_state (org_id);

    CREATE TABLE IF NOT EXISTS studio_audio_assets (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      audio_path TEXT NOT NULL,
      asset_type TEXT NOT NULL CHECK (asset_type IN ('waveform','spectrogram')),
      image_data BYTEA NOT NULL,
      mime_type TEXT NOT NULL DEFAULT 'image/png',
      width INT, height INT,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE (audio_path, asset_type)
    );

    CREATE INDEX IF NOT EXISTS studio_audio_assets_path_idx ON studio_audio_assets (audio_path);
    CREATE INDEX IF NOT EXISTS studio_audio_assets_type_idx ON studio_audio_assets (asset_type);
  ")

(defn ensure-schema!
  [pool]
  (pg/query! pool schema-ddl nil))

(defn- seed-tool-ids
  []
  (->> (concat (tool-registry/known-tool-ids)
               (seq (tool-registry/known-tool-ids)))
       (keep tool-registry/normalize-tool-id)
       distinct sort vec))

(defn insert-tool-seeds!
  [pool]
  (let [ids (seed-tool-ids)]
    (if (empty? ids)
      (js/Promise.resolve nil)
      (-> (js/Promise.all
           (into-array
            (for [tool-id ids]
              (let [{:keys [label description risk-level]} (tool-registry/get-tool tool-id)]
                (pg/query! pool
                  "INSERT INTO tool_definitions (id, label, description, risk_level)
                   VALUES ($1, $2, $3, $4)
                   ON CONFLICT (id) DO UPDATE
                   SET label = EXCLUDED.label,
                       description = EXCLUDED.description,
                       risk_level = EXCLUDED.risk_level"
                  [tool-id (or label tool-id) (or description "") (or risk-level "low")])))))
          (.then (fn [_] nil))))))

(defn insert-permission-seeds!
  "No-op — permissions are contract-driven."
  [_pool]
  (js/Promise.resolve nil))
