# Knoxx admin, multi-tenancy, and RBAC plan

## Goal

Turn Knoxx from a single-operator workbench into a product-grade multi-tenant system with:

- platform-wide administration for the primary company org
- org-scoped administration for customer/internal orgs
- built-in and custom roles
- per-role and per-user tool policies
- org-owned data lakes
- user and org lifecycle management
- auditable authorization decisions

## Current landed slice

The first backend control-plane slice is now present in the CLJS Knoxx backend:

- Postgres-backed control-plane bootstrap and seeding
- seeded primary org
- seeded bootstrap system admin user
- seeded built-in roles:
  - `system_admin`
  - `org_admin`
  - `knowledge_worker`
  - `data_analyst`
  - `developer`
- seeded permission atoms and tool definitions
- admin APIs for:
  - org creation/listing
  - user creation/listing
  - org role creation/listing
  - per-role tool-policy updates
  - per-membership role assignment
  - per-membership tool-policy overrides
  - data-lake creation/listing

Current implementation note:

- this is the **product control-plane foundation**, not the full runtime enforcement pass yet
- the next step is to wire conversation/run/memory/data-lake access through these org/user/membership policies instead of relying on process-local assumptions

## Design principles

1. **Authn and authz are separate**
   - identity answers: who is this principal?
   - authorization answers: what can they do, in which org, against which resources?

2. **Platform scope and org scope are distinct**
   - a platform operator can see and manage all orgs
   - an org admin can manage only resources inside their org

3. **Role grants and scope checks are separate**
   - TANF-app got this right: group permissions alone were not enough; object scope checks (STT/region) were a second layer
   - Knoxx should do the same with `org`, `data_lake`, `conversation`, `run`, and `tool` scope

4. **OpenPlanner is memory, not the policy database**
   - OpenPlanner should keep conversations, run receipts, and semantic memory
   - Knoxx authorization, tenancy, and policy records should live in relational storage
   - use the existing platform Postgres in `services/knoxx` as the canonical policy/config store

5. **Default deny, explicit allow**
   - access is denied unless granted by system role, org role, or explicit override
   - explicit deny overrides should exist for user-level exceptions and be audit logged

6. **Built-ins are templates, not a ceiling**
   - seed required roles
   - allow org admins and system admins to create custom roles from permission atoms

## TANF-app lessons worth copying

Relevant reference points:

- `orgs/riatzukiza/TANF-app/docs/Technical-Documentation/user_role_management.md`
- `orgs/riatzukiza/TANF-app/tdrs-backend/tdpservice/users/permissions.py`
- `orgs/riatzukiza/TANF-app/tdrs-frontend/src/components/PermissionGuard/PermissionGuard.jsx`
- `orgs/riatzukiza/TANF-app/tdrs-backend/tdpservice/users/test/test_permissions.py`
- `orgs/riatzukiza/TANF-app/tdrs-backend/docs/api/roles.md`

What to keep from that model:

- **roles as named permission bundles**
- **frontend guards driven by permission codenames**
- **runtime object-scope checks separate from role membership**
- **tests that assert exact permission envelopes for built-in roles**
- **admin surfaces for managing users and groups**

What not to copy literally:

- Knoxx should support **role creation/editing by API/UI**, not only admin-console mutation
- Knoxx needs **org and data-lake tenancy**, not only coarse group permissions
- Knoxx needs **tool policy** and **agent runtime policy**, which TANF-app did not have

## Core product model

### 1. Org model

There is one special org with platform authority.

- `primary org` = the company operating Knoxx as a product
- additional orgs = customers, internal business units, partner orgs, etc.

Recommended org kinds:

- `platform_owner`
- `customer`
- `internal`
- `partner`

System admins belong to the primary org but have platform-wide scope.

## Built-in roles

Seed these built-ins on bootstrap:

### System admin
Platform-wide role.

Can:
- create orgs
- create users across orgs
- assign system roles
- manage all org memberships
- manage all role templates
- manage all data lakes
- manage all tool policies
- view all audit logs
- support/impersonate with explicit audit trail
- read cross-org run/memory receipts where policy allows platform support access

### Org admin
Scoped to one org.

Can:
- manage users in their org
- assign org-scoped roles in their org
- create custom roles in their org
- manage org data lakes
- manage org tool policies
- manage org settings and branding
- view org audit logs

Cannot:
- create other orgs
- assign system admin
- view other orgs unless separately granted

### Knowledge worker
Default general-user role.

Can typically:
- use Knoxx chat/workbench
- read accessible data lakes
- use safe tools like read/search/scratchpad
- create artifacts inside allowed org spaces

Should not get shell/admin capabilities by default.

### Data analyst
Analysis-oriented role.

Can typically:
- do everything knowledge worker can do
- query analytic data lakes
- use retrieval/debug/inspection flows
- export results where org policy allows

Should not automatically get developer or org-admin powers.

### Developer
Builder/operator role inside an org.

Can typically:
- do everything knowledge worker can do
- use read/write/edit/bash and engineering tools
- access dev-oriented data lakes and workspaces allowed by policy

Should not automatically get user/org administration.

## Permission model

Use permission atoms, not hardcoded role-name checks.

Suggested families:

### Platform/org administration
- `platform.org.create`
- `platform.org.read`
- `platform.org.update`
- `platform.org.delete`
- `org.settings.read`
- `org.settings.update`
- `org.members.read`
- `org.members.create`
- `org.members.update`
- `org.members.delete`
- `org.users.invite`
- `org.users.create`
- `org.users.read`
- `org.users.update`
- `org.users.disable`

### Role and policy administration
- `platform.roles.manage`
- `org.roles.read`
- `org.roles.create`
- `org.roles.update`
- `org.roles.delete`
- `org.tool_policy.read`
- `org.tool_policy.update`
- `org.user_policy.read`
- `org.user_policy.update`

### Data lake administration and use
- `org.datalakes.read`
- `org.datalakes.create`
- `org.datalakes.update`
- `org.datalakes.delete`
- `datalake.query`
- `datalake.read`
- `datalake.write`
- `datalake.ingest`
- `datalake.admin`

### Agent/runtime permissions
- `agent.chat.use`
- `agent.memory.read`
- `agent.memory.cross_session`
- `agent.runs.read_own`
- `agent.runs.read_org`
- `agent.runs.read_all`
- `agent.controls.steer`
- `agent.controls.follow_up`

### Tool capability permissions
- `tool.read.use`
- `tool.write.use`
- `tool.edit.use`
- `tool.bash.use`
- `tool.email.send`
- `tool.discord.publish`
- `tool.bluesky.publish`
- `tool.semantic_query.use`
- `tool.memory_search.use`
- `tool.memory_session.use`

## Scope model

Every authorization decision should evaluate both:

1. **capability**: does the subject have the permission?
2. **scope**: is the target resource in a scope they are allowed to touch?

Resource scopes:

- platform
- org
- data lake
- conversation
- run
- tool
- workspace path

Examples:

- org admin may have `org.members.update`, but only for memberships where `membership.org_id == current_org_id`
- data analyst may have `datalake.query`, but only for lakes granted to their org membership
- developer may have `tool.bash.use`, but only inside workspace roots approved by the org/tool policy

## Policy resolution

Recommended effective-policy order:

1. system admin bypass / platform emergency override
2. explicit user deny
3. explicit role deny
4. explicit user allow
5. role allow
6. default deny

Keep deny support narrow and auditable.

## Relational schema proposal

Use Postgres for the product control plane.

### Core tables

- `orgs`
  - `id`
  - `slug`
  - `name`
  - `kind`
  - `is_primary`
  - `status`
  - `created_at`
  - `updated_at`

- `users`
  - `id`
  - `email`
  - `display_name`
  - `external_subject`
  - `auth_provider`
  - `status`
  - `created_at`
  - `updated_at`

- `memberships`
  - `id`
  - `user_id`
  - `org_id`
  - `status`
  - `is_default`
  - `created_at`
  - `updated_at`

- `roles`
  - `id`
  - `org_id` nullable for platform/global roles
  - `name`
  - `slug`
  - `scope_kind` (`platform` or `org`)
  - `built_in`
  - `system_managed`
  - `created_at`
  - `updated_at`

- `permissions`
  - `id`
  - `code`
  - `resource_kind`
  - `action`
  - `description`

- `role_permissions`
  - `role_id`
  - `permission_id`
  - `effect` (`allow` or `deny`)

- `membership_roles`
  - `membership_id`
  - `role_id`

### Overrides and policy tables

- `user_permission_overrides`
  - `membership_id`
  - `permission_id`
  - `effect`

- `tool_definitions`
  - `id`
  - `label`
  - `risk_level`
  - `config_schema`

- `role_tool_policies`
  - `role_id`
  - `tool_id`
  - `effect`
  - `constraints_json`

- `user_tool_policies`
  - `membership_id`
  - `tool_id`
  - `effect`
  - `constraints_json`

### Data lake tables

- `data_lakes`
  - `id`
  - `org_id`
  - `name`
  - `slug`
  - `kind` (`workspace_docs`, `openplanner_memory`, `qdrant_collection`, `warehouse`, `external_api`)
  - `config_json`
  - `status`
  - `created_at`
  - `updated_at`

- `role_data_lake_policies`
  - `role_id`
  - `data_lake_id`
  - `can_read`
  - `can_query`
  - `can_write`
  - `can_ingest`
  - `can_admin`

- `user_data_lake_policies`
  - `membership_id`
  - `data_lake_id`
  - same capability columns or `policy_json`

### Audit tables

- `audit_events`
  - `id`
  - `actor_user_id`
  - `actor_membership_id`
  - `org_id`
  - `action`
  - `resource_kind`
  - `resource_id`
  - `before_json`
  - `after_json`
  - `created_at`

## Runtime integration points

Knoxx runtime objects should gain these fields:

- `org_id`
- `user_id`
- `membership_id`
- `effective_role_ids`
- `active_data_lake_id`

OpenPlanner event indexing should also include those tags so memory search can enforce tenant boundaries.

That means:
- prior sessions are searchable within the org by default
- system admins can search across orgs with explicit platform permissions
- cross-org bleed is blocked by default

## API surface to add

### Auth/session context
- `GET /api/me`
- `GET /api/me/memberships`
- `POST /api/me/switch-org`

### Org admin
- `GET /api/admin/orgs`
- `POST /api/admin/orgs`
- `GET /api/admin/orgs/:orgId`
- `PATCH /api/admin/orgs/:orgId`

### User and membership admin
- `GET /api/admin/orgs/:orgId/users`
- `POST /api/admin/orgs/:orgId/users`
- `POST /api/admin/orgs/:orgId/invitations`
- `PATCH /api/admin/memberships/:membershipId`
- `POST /api/admin/memberships/:membershipId/roles`

### Role admin
- `GET /api/admin/orgs/:orgId/roles`
- `POST /api/admin/orgs/:orgId/roles`
- `PATCH /api/admin/roles/:roleId`
- `GET /api/admin/permissions`

### Tool policy admin
- `GET /api/admin/orgs/:orgId/tools`
- `PATCH /api/admin/roles/:roleId/tool-policies`
- `PATCH /api/admin/memberships/:membershipId/tool-policies`

### Data lake admin
- `GET /api/admin/orgs/:orgId/data-lakes`
- `POST /api/admin/orgs/:orgId/data-lakes`
- `PATCH /api/admin/data-lakes/:lakeId`
- `PATCH /api/admin/roles/:roleId/data-lake-policies`

### Audit/admin visibility
- `GET /api/admin/orgs/:orgId/audit`
- `GET /api/admin/audit`

## UI shape

Add a new admin surface in Knoxx with tabs roughly like:

### Platform admin
Visible only to system admins.

- orgs
- users
- global roles
- global permissions
- audit log

### Org admin
Visible to org admins in their org.

- members
- roles
- data lakes
- tool policies
- org settings
- audit log

### Role editor
- select permission atoms
- assign tool policies
- assign data-lake policies
- preview effective access

### Membership editor
- assign org roles
- add user-specific allows/denies
- set default data lake
- disable user or suspend membership

## Recommended first implementation slices

### Slice 1: authz foundation
- add Postgres-backed org/user/membership/role/permission tables
- seed built-in roles and permission atoms
- create a request context resolver that attaches `current-user`, `current-org`, and `effective-permissions`

### Slice 2: replace hardcoded role-tools
- move current `role-tools` map out of code and into seeded tool policy records
- keep the current built-ins as seeded defaults:
  - map `executive` concept toward `knowledge_worker`
  - add `data_analyst`
  - add `developer`
  - add `org_admin`
  - add `system_admin`

### Slice 3: data lake tenancy
- make current `database-state*` profiles org-owned rather than process-global
- store them in Postgres
- enforce org and membership access on every data-lake route

### Slice 4: admin APIs and UI
- org list/create
- user create/invite
- role create/edit
- per-role tool policy editor
- per-user override editor

### Slice 5: org-scoped memory and runs
- tag runs and OpenPlanner events with org/user/membership
- restrict `/api/memory/*` and `/api/runs/*` by org scope
- add cross-org support only for system admins

## Verification strategy

Follow the TANF-app pattern of permission tests, but expand to tenancy and tool policy.

Need tests for:

- system admin can manage all orgs
- org admin can manage only own org
- knowledge worker cannot access admin APIs
- data analyst can query allowed lakes but not administer them
- developer can use allowed tools but not org admin surfaces
- per-user deny removes one tool granted by role
- cross-org memory search is denied by default
- system admin cross-org memory search is allowed and audit logged
- org deletion/disable blocks new sessions but preserves auditability

## Immediate architectural conclusion

The next serious Knoxx step is **not** another ad hoc settings atom.
It is a real product control plane:

- Postgres for org/user/role/policy state
- OpenPlanner for memory and receipts
- permission atoms + scope checks
- admin UI on top of that

That gives you the company-operated, multi-org Knoxx product shape you described, while keeping the TANF-app lesson that fine-grained permissions and object scope must both be enforced.
