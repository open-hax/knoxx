---
uuid: "knoxx-ingestion-github-driver"
title: "Ingestion GitHub Driver"
status: todo
priority: P2
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-28T00:00:00Z"
source: "specs/epics/knowledge-ops-ingestion-pipeline.md"
points: 5
category: tasks
---

# Ingestion GitHub Driver

> Parent: `knowledge-ops-ingestion-pipeline`
> Points: 5

## Purpose

Add a GitHub driver that can ingest code, issues, PRs, and wikis from GitHub organization repositories.

## Current State

The driver registry (`drivers/registry.clj`) has a placeholder comment: `;; "github" github/create-driver`. The driver protocol is defined. No GitHub driver exists.

## What Needs Building

1. **Driver implementation** — implement `Driver` protocol for GitHub.
2. **Discovery** — list repos in org, walk file tree, list issues/PRs.
3. **Extraction** — fetch file contents, issue bodies, PR descriptions + review comments.
4. **Rate limiting** — respect GitHub API rate limits (5000 req/hr for authenticated).
5. **Auth** — GitHub PAT from config.
6. **State** — track last scan timestamp per repo for incremental discovery.

## Config Schema

```clojure
{:org "open-hax"                    ; required
 :repos []                          ; empty = all repos
 :token "ghp_..."                   ; required, GitHub PAT
 :include-issues true
 :include-prs true
 :include-discussions false
 :include-wiki true
 :file-types [".md" ".cljs" ".clj" ".ts"]}
```

## Acceptance Criteria

- [ ] Driver implements `discover`, `extract`, `extract-batch`, `get-state`, `set-state`
- [ ] Discovery lists repos, walks file trees, lists issues/PRs
- [ ] Extraction returns file content, issue body, PR description
- [ ] Rate limiting respected (backoff on 429)
- [ ] Incremental discovery works (only changed files since last scan)
- [ ] Registered in `drivers/registry.clj`

## Implementation Notes

- Use `clj-http.client` for HTTP
- GitHub API v3: `https://api.github.com`
- Content API: `GET /repos/{owner}/{repo}/contents/{path}`
- Issues API: `GET /repos/{owner}/{repo}/issues`
- PRs API: `GET /repos/{owner}/{repo}/pulls`
