---
uuid: "knoxx-store-protocol"
title: "Store Protocol (IStore)"
status: done
priority: "P2"
labels: ["tasks", "5sp", "resource-architecture"]
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: 5
category: "tasks"
---
# Store Protocol (IStore)

> Parent epic: `knoxx-action-scope-and-pipeline-collapse`

## Context

Stores are keyed persistence with schemas. Defined via `:store/id` and `:store/schema`, instantiated via `IStore` protocol.

## Work

1. Create `IStore` protocol:
   ```clojure
   (defprotocol IStore
     (-insert [this doc])
     (-find [this query]))
   ```

2. Create `MongoCollection` record implementing `IStore`
3. Create `law/store.cljs` with `compile-schema-guard` for Malli validation
4. Create store registry to instantiate stores from resource definitions
5. Wire stores into scope resolution
6. Add tests

## Definition of Done

- [ ] `IStore` protocol defined
- [ ] `MongoCollection` record implements it
- [ ] `:store/id` + `:store/schema` instantiate stores
- [ ] Stores available in action scope
- [ ] Tests pass

## Risks

- MongoDB integration complexity
- Schema validation performance
