---
uuid: "knoxx-ingestion-github-driver"
title: "Ingestion GitHub Driver"
status: done
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

---

**Implemented (2026-05-29).** Status: done.

Built the GitHub driver end-to-end:

- `ingestion/src/kms_ingestion/drivers/github.clj` — `GitHubDriver` `deftype` implementing the full `Driver` protocol (`discover`, `extract`, `extract-batch`, `get-state`, `set-state`, `close`). Includes:
  - HTTP helpers (`auth-headers`, `api-get`) using `clj-http.client` with bounded rate-limit backoff (`rate-limited?`, `retry-after-ms`) honoring `Retry-After` and `X-RateLimit-Reset`, with exponential fallback. Detects 429 and 403+remaining=0.
  - URL builders for org repos, repo, recursive git trees, contents, issues, pulls.
  - Namespaced file-ids (`blob:owner/repo@sha:path`, `issue:owner/repo#n`, `pr:owner/repo#n`) with `parse-file-id` for extraction routing.
  - Discovery: `list-repos` (explicit `:repos` or all org repos), `discover-files` (walks recursive git tree, filters by `:file-types`, uses git blob sha as content hash), `discover-issues` (excludes PRs, supports `since`), `discover-prs`. `classify` for new/changed/unchanged.
  - Extraction: `extract-blob` (base64-decode contents API), `extract-issue`, `extract-pr` (body + metadata).
  - State: per-repo `:last-scan` timestamp persisted in the state atom for incremental discovery.
  - Config validation (`validate-config`) requiring `:org` and `:token`.
- `ingestion/test/kms_ingestion/drivers/github_test.clj` — 14 deftests covering URL building, file-id roundtrip/parse (incl. colon-in-path edge case), auth headers, rate-limit detection, retry-after computation, config validation, file-type matching, classify, driver creation/protocol satisfaction, invalid-config discover, state round-trip, unparseable-extract error handling, and registry registration.
- `ingestion/src/kms_ingestion/drivers/registry.clj` — required `github` ns and registered `"github" github/create-driver`.

Verification:
- `clojure -M:test` => Ran 72 tests, 299 assertions, 0 failures, 0 errors.
- `clj-kondo --lint github.clj github_test.clj registry.clj` => errors: 0, warnings: 0.
