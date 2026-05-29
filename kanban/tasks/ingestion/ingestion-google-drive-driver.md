---
uuid: "knoxx-ingestion-google-drive-driver"
title: "Ingestion Google Drive Driver"
status: icebox
priority: P3
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-28T00:00:00Z"
source: "specs/epics/knowledge-ops-ingestion-pipeline.md"
points: 5
category: tasks
---

# Ingestion Google Drive Driver

> Parent: `knowledge-ops-ingestion-pipeline`
> Points: 5
> Status: icebox — not a current priority

## Purpose

Add a Google Drive driver that can ingest documents, spreadsheets, and presentations from Google Drive.

## What Needs Building

1. **Driver implementation** — implement `Driver` protocol for Google Drive.
2. **OAuth2 auth** — Google service account or OAuth2 credentials.
3. **Discovery** — list files in folder tree, filter by MIME type.
4. **Extraction** — export Google Docs to text, Sheets to CSV, Slides to text.
5. **State** — track page tokens for incremental discovery.

## Config Schema

```clojure
{:credentials {:type "service_account" ...}  ; or OAuth2
 :root-folder-id "..."                        ; optional, start from specific folder
 :include-shared true
 :include-trashed false
 :export-formats {"application/vnd.google-apps.document" "text/plain"
                  "application/vnd.google-apps.spreadsheet" "text/csv"}}
```

## Acceptance Criteria

- [ ] Driver implements `Driver` protocol
- [ ] Discovery lists files with pagination
- [ ] Exports Google Docs/Sheets/Slides to text
- [ ] Incremental discovery via page tokens
- [ ] Registered in `drivers/registry.clj`

## Blocked By

- Google Cloud service account setup
- Decision on auth method (service account vs OAuth2)
