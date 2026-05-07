# Garden CMS Playlist Chat + Label Provenance

Status: draft
Owner: Knoxx / OpenPlanner Gardens / Broadcast Studio
Created: 2026-05-07

## Intent

Published garden playlist pages should feel like a focused Broadcast Studio playlist/now-playing surface, not a generic markdown article with a file explorer.

## Garden playlist view rules

- A published garden CMS view has no workspace explorer.
- Playlist publications render with a Studio-like now-playing/playlist layout:
  - prominent current/hero audio block;
  - ordered queue/track list;
  - labels and heard descriptions where available;
  - bounded initial render for large playlists;
  - complete playlist data remains in structured block metadata.
- Garden chat is an optional CMS feature block/panel.
- Each garden has its own actor, e.g. `garden:<garden_id>` or a contract-backed actor file.
- If chat is enabled in a published garden view:
  - no model selection is shown to public users;
  - available agents are exactly the agents configured for that garden actor;
  - the page runtime sends garden/document context as fixed template context.

## CMS editor panel rules

- Any secondary chat panel should use the same collapse affordance as Broadcast Studio.
- The CMS editor can show file explorer internally, but the published garden view never exposes it.

## Label provenance rules

- Label decisions are authoritative only when attributed to authorized user actors.
- Labels need counts and provenance:
  - number of nodes carrying each label;
  - label edge source (`studio`, `api`, `discord`, etc.);
  - actor id that applied the label;
  - external account identity where available.
- Existing unattributed labels are attributed to `foamy125_gmail_com` as the migration baseline.
- Dynamic actor creation should occur for unique Discord, Bluesky, and Twitch accounts encountered by API integrations.
- Actors are graph nodes.
- When possible, add attribution edges from actors to contributed nodes/edges.

## Actor account schema

Actors may include:

```edn
:actor/accounts {:discord {:username "..." :user-id "..."}
                 :bluesky {:handle "..." :did "..."}
                 :twitch {:username "..." :user-id "..."}}
```

Canonical foamy account links:

```edn
:actor/accounts {:discord {:username "error0815"
                           :user-id "205909976768708608"}
                 :bluesky {:handle "@37707.bsky.social"}}
```

## Implementation phases

1. Shared secondary-panel collapse affordance across CMS/Studio chat panels.
2. Garden-scoped playlist render mode in OpenPlanner garden renderer.
3. Garden actor mapping and no-model-select public chat block.
4. Label count/provenance API extensions.
5. Actor upsert/migration for external accounts and graph attribution edges.
