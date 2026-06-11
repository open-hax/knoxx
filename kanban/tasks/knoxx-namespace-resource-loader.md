---
uuid: "knoxx-namespace-resource-loader"
title: "Namespace Resource Loader"
status: done
priority: "P1"
labels: ["tasks", "5sp", "resource-architecture"]
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: 5
category: "tasks"
---
# Namespace Resource Loader

> Parent epic: `knoxx-action-scope-and-pipeline-collapse`

## Context

Resources currently live in individual EDN files with `:contract/id`. The target is namespace files with `:namespace` + `:resources` vector. Identity: `:namespace` + resource local id → qualified id.

## Work

1. Define namespace file schema:
   ```clojure
   {:namespace :ussyverse
    :resources [{:trigger/id :discord.mentions ...}
                {:action/id :custom-reply ...}]}
   ```

2. Update `domain/resources/loader.cljs` to read namespace files
3. Assign qualified ids: `:namespace :ussyverse` + `:trigger/id :discord.mentions` → `:ussyverse/discord.mentions`
4. Maintain backward compat with individual `:contract/id` files
5. Add tests for namespace loading and id resolution

## Definition of Done

- [ ] Namespace files load correctly
- [ ] Qualified ids assigned (`:namespace/resource-id`)
- [ ] Backward compat with individual files preserved
- [ ] Tests pass

## Risks

- Large loader refactor
- Backward compat with existing individual files
