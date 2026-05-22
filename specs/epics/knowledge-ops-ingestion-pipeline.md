# Knowledge Ops — Ingestion Pipeline Spec

> *The driver is the gateway. The queue is the memory. The stream is the pulse.*

---

## Purpose

Define a driver-based ingestion system that can import knowledge from multiple sources (local filesystem, cloud storage, code repos, ticketing systems) with state tracking, progress streaming, and resume capability.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    INGESTION PIPELINE                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐       │
│  │ DRIVER  │───▶│  QUEUE  │───▶│ WORKER  │───▶│ QDRANT  │       │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘       │
│       │              │              │              │             │
│       ▼              ▼              ▼              ▼             │
│   DISCOVER      JOB STATUS     CHUNK/EMBED    COLLECTION         │
│   EXTRACT       PROGRESS       INGEST         METADATA           │
│   TRACK STATE   ERRORS         BATCH UPSERT   VECTORS            │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                  WEBSOCKET / SSE                          │    │
│  │              Real-time progress streaming                 │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Driver Interface

### Abstract Driver

```python
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import AsyncIterator, Any

class DriverType(str, Enum):
    LOCAL = "local"
    SFTP = "sftp"
    GITHUB = "github"
    GITLAB = "gitlab"
    BITBUCKET = "bitbucket"
    JIRA = "jira"
    TRELLO = "trello"
    NOTION = "notion"
    GOOGLE_DRIVE = "google_drive"
    MICROSOFT_TEAMS = "microsoft_teams"


@dataclass
class SourceFile:
    """A file/document discovered by a driver."""
    id: str                          # Unique identifier in source system
    path: str                        # Display path/title
    content: str | None = None       # Extracted text content
    content_hash: str | None = None  # SHA-256 of content for change detection
    metadata: dict = field(default_factory=dict)
    size: int = 0
    modified_at: datetime | None = None
    author: str | None = None
    url: str | None = None           # Link back to source


@dataclass
class DriverConfig:
    """Configuration for a driver instance."""
    driver_type: DriverType
    tenant_id: str
    name: str                        # Human-readable name for this source
    config: dict = field(default_factory=dict)  # Driver-specific config
    collections: list[str] = field(default_factory=lambda: ["devel_docs"])
    file_types: list[str] | None = None  # Filter by file type
    include_patterns: list[str] | None = None  # Glob patterns to include
    exclude_patterns: list[str] | None = None  # Glob patterns to exclude
    enabled: bool = True


@dataclass
class DiscoveryResult:
    """Result of a driver discovery scan."""
    total_files: int
    new_files: int
    changed_files: int
    deleted_files: int
    unchanged_files: int
    files: list[SourceFile]


class BaseDriver(ABC):
    """Abstract base class for all ingestion drivers."""

    def __init__(self, config: DriverConfig):
        self.config = config

    @abstractmethod
    async def discover(self, since: datetime | None = None) -> DiscoveryResult:
        """
        Discover files in the source.

        If `since` is provided, only return files changed since that time.
        Otherwise, return all discoverable files.
        """
        pass

    @abstractmethod
    async def extract(self, file_id: str) -> SourceFile:
        """Extract content for a specific file."""
        pass

    @abstractmethod
    async def extract_batch(self, file_ids: list[str]) -> list[SourceFile]:
        """Extract content for multiple files (for efficiency)."""
        pass

    @abstractmethod
    async def get_state(self) -> dict[str, Any]:
        """
        Get driver-specific state for persistence.

        This might include: last scan timestamp, cursor positions,
        sync tokens, etc.
        """
        pass

    @abstractmethod
    async def set_state(self, state: dict[str, Any]) -> None:
        """Restore driver state from persistence."""
        pass

    async def close(self) -> None:
        """Clean up resources (optional)."""
        pass
```

---

## Job Queue

### Database Schema

```sql
-- Ingestion sources (driver instances)
CREATE TABLE ingestion_sources (
    source_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       TEXT NOT NULL REFERENCES tenants(tenant_id),
    driver_type     TEXT NOT NULL,
    name            TEXT NOT NULL,
    config          JSONB NOT NULL DEFAULT '{}',
    state           JSONB DEFAULT '{}',           -- Driver-specific state
    last_scan_at    TIMESTAMPTZ,
    last_error      TEXT,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ingestion_sources_tenant ON ingestion_sources(tenant_id);
CREATE INDEX idx_ingestion_sources_type ON ingestion_sources(driver_type);

-- Ingestion jobs
CREATE TABLE ingestion_jobs (
    job_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id       UUID NOT NULL REFERENCES ingestion_sources(source_id),
    tenant_id       TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'pending'
                        CHECK (status IN ('pending', 'running', 'completed', 'failed', 'cancelled')),
    total_files     INTEGER DEFAULT 0,
    processed_files INTEGER DEFAULT 0,
    failed_files    INTEGER DEFAULT 0,
    skipped_files   INTEGER DEFAULT 0,
    chunks_created  INTEGER DEFAULT 0,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    error_message   TEXT,
    config          JSONB DEFAULT '{}',           -- Job-specific overrides
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ingestion_jobs_source ON ingestion_jobs(source_id);
CREATE INDEX idx_ingestion_jobs_status ON ingestion_jobs(status);
CREATE INDEX idx_ingestion_jobs_tenant ON ingestion_jobs(tenant_id);

-- Ingestion file state (what's been ingested)
CREATE TABLE ingestion_file_state (
    file_id         TEXT PRIMARY KEY,             -- Driver's file ID
    source_id       UUID NOT NULL REFERENCES ingestion_sources(source_id),
    tenant_id       TEXT NOT NULL,
    path            TEXT NOT NULL,
    content_hash    TEXT NOT NULL,                -- SHA-256 of content
    status          TEXT NOT NULL DEFAULT 'ingested'
                        CHECK (status IN ('pending', 'ingested', 'failed', 'deleted')),
    chunks          INTEGER DEFAULT 0,
    collections     JSONB DEFAULT '[]',           -- Which Qdrant collections
    last_ingested_at TIMESTAMPTZ,
    error_message   TEXT,
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ingestion_file_state_source ON ingestion_file_state(source_id);
CREATE INDEX idx_ingestion_file_state_hash ON ingestion_file_state(content_hash);
CREATE INDEX idx_ingestion_file_state_status ON ingestion_file_state(status);
```

---

## API Surface

### Sources API

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/ingestion/sources` | GET | List all configured sources |
| `/api/ingestion/sources` | POST | Create a new source |
| `/api/ingestion/sources/{id}` | GET | Get source details + state |
| `/api/ingestion/sources/{id}` | PATCH | Update source config |
| `/api/ingestion/sources/{id}` | DELETE | Delete source + state |
| `/api/ingestion/sources/{id}/discover` | POST | Run discovery scan |
| `/api/ingestion/sources/{id}/sync` | POST | Sync (discover + ingest changes) |

### Jobs API

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/ingestion/jobs` | GET | List jobs (filter by source, status) |
| `/api/ingestion/jobs/{id}` | GET | Get job details + progress |
| `/api/ingestion/jobs/{id}/cancel` | POST | Cancel running job |
| `/api/ingestion/jobs/{id}/retry` | POST | Retry failed job |

### Bulk Import API

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/ingestion/bulk` | POST | Start bulk import job |
| `/api/ingestion/bulk/{id}` | GET | Get bulk import status |
| `/api/ingestion/upload` | POST | Upload files directly |

### WebSocket / SSE

| Endpoint | Purpose |
|----------|---------|
| `/ws/ingestion/jobs/{id}` | WebSocket for job progress |
| `/api/ingestion/jobs/{id}/stream` | SSE for job progress |

---

## Progress Events

```typescript
interface IngestionProgressEvent {
  type: "progress" | "file_start" | "file_complete" | "file_error" | "job_complete" | "job_error";
  job_id: string;
  timestamp: string;

  // For progress events
  total_files?: number;
  processed_files?: number;
  failed_files?: number;
  skipped_files?: number;
  chunks_created?: number;
  percent_complete?: number;

  // For file events
  file_id?: string;
  file_path?: string;
  file_status?: "success" | "failed" | "skipped";
  file_chunks?: number;
  file_error?: string;

  // For job events
  status?: "completed" | "failed" | "cancelled";
  error_message?: string;
  duration_seconds?: number;
}
```

---

## Driver Implementations

### Local Driver

```python
class LocalDriver(BaseDriver):
    """Ingest from local filesystem."""

    config_schema = {
        "root_path": {"type": "string", "required": True},
        "watch": {"type": "boolean", "default": False},  # Enable file watching
        "follow_symlinks": {"type": "boolean", "default": False},
    }

    async def discover(self, since: datetime | None = None) -> DiscoveryResult:
        root = Path(self.config.config["root_path"])

        # Walk filesystem
        for path in root.rglob("*"):
            if not path.is_file():
                continue
            if self._should_skip(path):
                continue

            stat = path.stat()
            file_hash = self._compute_hash(path)

            # Check if changed since last ingestion
            existing = await self._get_existing_state(str(path))
            if existing and existing["content_hash"] == file_hash:
                # Unchanged
                continue

            yield SourceFile(
                id=str(path),
                path=str(path.relative_to(root)),
                content_hash=file_hash,
                size=stat.st_size,
                modified_at=datetime.fromtimestamp(stat.st_mtime),
            )
```

### GitHub Driver

```python
class GitHubDriver(BaseDriver):
    """Ingest from GitHub organization repositories."""

    config_schema = {
        "org": {"type": "string", "required": True},
        "repos": {"type": "array", "items": {"type": "string"}},  # Empty = all
        "token": {"type": "string", "required": True},  # GitHub PAT
        "include_issues": {"type": "boolean", "default": True},
        "include_prs": {"type": "boolean", "default": True},
        "include_discussions": {"type": "boolean", "default": False},
        "include_wiki": {"type": "boolean", "default": True},
    }

    async def discover(self, since: datetime | None = None) -> DiscoveryResult:
        async with httpx.AsyncClient() as client:
            # Get all repos in org
            repos = await self._list_repos(client)

            for repo in repos:
                # Get repo contents (code files)
                async for file in self._walk_repo(client, repo, since):
                    yield file

                # Get issues
                if self.config.config.get("include_issues"):
                    async for issue in self._list_issues(client, repo, since):
                        yield issue

                # Get PRs
                if self.config.config.get("include_prs"):
                    async for pr in self._list_prs(client, repo, since):
                        yield pr
```

### Google Drive Driver

```python
class GoogleDriveDriver(BaseDriver):
    """Ingest from Google Drive."""

    config_schema = {
        "credentials": {"type": "object", "required": True},  # OAuth credentials
        "root_folder_id": {"type": "string"},  # Optional: start from specific folder
        "include_shared": {"type": "boolean", "default": True},
        "include_trashed": {"type": "boolean", "default": False},
        "export_formats": {
            "type": "object",
            "default": {
                "application/vnd.google-apps.document": "text/plain",
                "application/vnd.google-apps.spreadsheet": "text/csv",
                "application/vnd.google-apps.presentation": "text/plain",
            }
        },
    }

    async def discover(self, since: datetime | None = None) -> DiscoveryResult:
        service = build("drive", "v3", credentials=self.credentials)

        query = "trashed=false"
        if self.config.config.get("root_folder_id"):
            query += f" and '{root_folder_id}' in parents"
        if since:
            query += f" and modifiedTime > '{since.isoformat()}'"

        page_token = None
        while True:
            results = service.files().list(
                q=query,
                fields="nextPageToken, files(id, name, mimeType, modifiedTime, size)",
                pageToken=page_token,
            ).execute()

            for file in results.get("files", []):
                yield SourceFile(
                    id=file["id"],
                    path=file["name"],
                    metadata={"mime_type": file["mimeType"]},
                    modified_at=datetime.fromisoformat(file["modifiedTime"].replace("Z", "+00:00")),
                )

            page_token = results.get("nextPageToken")
            if not page_token:
                break
```

---

## Worker Implementation

```python
class IngestionWorker:
    """Process ingestion jobs from the queue."""

    def __init__(self, ragussy_url: str, qdrant_url: str, db_pool):
        self.ragussy_url = ragussy_url
        self.qdrant_url = qdrant_url
        self.db_pool = db_pool
        self.active_jobs: dict[str, asyncio.Task] = {}

    async def start_job(self, job_id: str) -> None:
        """Start processing a job."""
        if job_id in self.active_jobs:
            return

        self.active_jobs[job_id] = asyncio.create_task(self._process_job(job_id))

    async def _process_job(self, job_id: str) -> None:
        """Process an ingestion job."""
        async with self.db_pool.acquire() as conn:
            # Get job details
            job = await conn.fetchrow(
                "SELECT * FROM ingestion_jobs WHERE job_id = $1",
                job_id
            )

            # Get source and driver
            source = await conn.fetchrow(
                "SELECT * FROM ingestion_sources WHERE source_id = $1",
                job["source_id"]
            )

            driver = self._create_driver(source)

            # Update job status
            await conn.execute(
                "UPDATE ingestion_jobs SET status = 'running', started_at = NOW() WHERE job_id = $1",
                job_id
            )

            # Broadcast start event
            await self._broadcast_progress(job_id, {
                "type": "progress",
                "status": "running",
                "processed_files": 0,
                "total_files": 0,
            })

            try:
                # Discover files
                discovery = await driver.discover(since=source.get("last_scan_at"))

                # Update totals
                await conn.execute(
                    "UPDATE ingestion_jobs SET total_files = $1 WHERE job_id = $2",
                    discovery.total_files,
                    job_id
                )

                # Process files in batches
                batch_size = 10
                processed = 0
                failed = 0
                chunks_total = 0

                for i in range(0, len(discovery.files), batch_size):
                    batch = discovery.files[i:i + batch_size]

                    # Extract content
                    files_with_content = await driver.extract_batch([f.id for f in batch])

                    # Chunk and embed via Ragussy
                    for file in files_with_content:
                        try:
                            result = await self._ingest_file(file, job)
                            chunks_total += result["chunks"]
                            processed += 1

                            # Update file state
                            await conn.execute("""
                                INSERT INTO ingestion_file_state
                                    (file_id, source_id, tenant_id, path, content_hash, status, chunks, collections)
                                VALUES ($1, $2, $3, $4, $5, 'ingested', $6, $7)
                                ON CONFLICT (file_id) DO UPDATE SET
                                    content_hash = $5,
                                    status = 'ingested',
                                    chunks = $6,
                                    last_ingested_at = NOW(),
                                    updated_at = NOW()
                            """, file.id, source["source_id"], job["tenant_id"],
                                file.path, file.content_hash, result["chunks"],
                                json.dumps(job["config"].get("collections", ["devel_docs"])))

                            # Broadcast progress
                            await self._broadcast_progress(job_id, {
                                "type": "file_complete",
                                "file_id": file.id,
                                "file_path": file.path,
                                "file_status": "success",
                                "file_chunks": result["chunks"],
                                "processed_files": processed,
                                "total_files": discovery.total_files,
                                "percent_complete": (processed / discovery.total_files) * 100,
                            })

                        except Exception as e:
                            failed += 1
                            await self._broadcast_progress(job_id, {
                                "type": "file_error",
                                "file_id": file.id,
                                "file_path": file.path,
                                "file_status": "failed",
                                "file_error": str(e),
                            })

                # Mark job complete
                await conn.execute("""
                    UPDATE ingestion_jobs SET
                        status = 'completed',
                        processed_files = $1,
                        failed_files = $2,
                        chunks_created = $3,
                        completed_at = NOW()
                    WHERE job_id = $4
                """, processed, failed, chunks_total, job_id)

                # Update source last_scan
                await conn.execute(
                    "UPDATE ingestion_sources SET last_scan_at = NOW() WHERE source_id = $1",
                    source["source_id"]
                )

                await self._broadcast_progress(job_id, {
                    "type": "job_complete",
                    "status": "completed",
                    "processed_files": processed,
                    "failed_files": failed,
                    "chunks_created": chunks_total,
                })

            except Exception as e:
                await conn.execute(
                    "UPDATE ingestion_jobs SET status = 'failed', error_message = $1 WHERE job_id = $2",
                    str(e), job_id
                )

                await self._broadcast_progress(job_id, {
                    "type": "job_error",
                    "status": "failed",
                    "error_message": str(e),
                })

            finally:
                await driver.close()
                del self.active_jobs[job_id]
```

---

## WebSocket Server

```python
from fastapi import WebSocket
from typing import Dict, Set

class ProgressBroadcaster:
    """Broadcast ingestion progress to connected clients."""

    def __init__(self):
        self.connections: Dict[str, Set[WebSocket]] = {}

    async def connect(self, job_id: str, websocket: WebSocket):
        await websocket.accept()
        if job_id not in self.connections:
            self.connections[job_id] = set()
        self.connections[job_id].add(websocket)

    async def disconnect(self, job_id: str, websocket: WebSocket):
        if job_id in self.connections:
            self.connections[job_id].discard(websocket)
            if not self.connections[job_id]:
                del self.connections[job_id]

    async def broadcast(self, job_id: str, message: dict):
        if job_id not in self.connections:
            return

        dead_connections = set()
        for websocket in self.connections[job_id]:
            try:
                await websocket.send_json(message)
            except:
                dead_connections.add(websocket)

        for ws in dead_connections:
            await self.disconnect(job_id, ws)


# FastAPI routes
@router.websocket("/ws/ingestion/jobs/{job_id}")
async def ingestion_progress_ws(websocket: WebSocket, job_id: str):
    await broadcaster.connect(job_id, websocket)
    try:
        while True:
            # Keep connection alive, wait for client messages
            data = await websocket.receive_text()
            if data == "ping":
                await websocket.send_json({"type": "pong"})
    except:
        pass
    finally:
        await broadcaster.disconnect(job_id, websocket)


@router.get("/api/ingestion/jobs/{job_id}/stream")
async def ingestion_progress_sse(request: Request, job_id: str):
    async def event_stream():
        queue = asyncio.Queue()

        # Subscribe to job updates
        async def on_progress(msg):
            await queue.put(msg)

        subscriber_id = await subscribe_to_job(job_id, on_progress)

        try:
            while True:
                msg = await asyncio.wait_for(queue.get(), timeout=30.0)
                yield f"data: {json.dumps(msg)}\n\n"
        except asyncio.TimeoutError:
            yield f": heartbeat\n\n"
        finally:
            await unsubscribe_from_job(job_id, subscriber_id)

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "Connection": "keep-alive"},
    )
```

---

## Frontend Components

### Ingestion Dashboard

```tsx
interface IngestionSource {
  source_id: string;
  driver_type: string;
  name: string;
  last_scan_at: string | null;
  status: "idle" | "scanning" | "syncing" | "error";
  total_files: number;
  enabled: boolean;
}

function IngestionDashboard() {
  const [sources, setSources] = useState<IngestionSource[]>([]);
  const [selectedSource, setSelectedSource] = useState<IngestionSource | null>(null);
  const [jobs, setJobs] = useState<IngestionJob[]>([]);

  useEffect(() => {
    loadSources();
    loadJobs();
  }, []);

  const handleCreateSource = async (config: CreateSourcePayload) => {
    const resp = await fetch("/api/ingestion/sources", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(config),
    });
    if (resp.ok) {
      loadSources();
    }
  };

  const handleStartSync = async (sourceId: string) => {
    const resp = await fetch(`/api/ingestion/sources/${sourceId}/sync`, {
      method: "POST",
    });
    if (resp.ok) {
      const job = await resp.json();
      navigate(`/ingestion/jobs/${job.job_id}`);
    }
  };

  return (
    <div className="flex h-full">
      {/* Sources sidebar */}
      <div className="w-64 border-r p-4">
        <h2 className="font-semibold mb-4">Data Sources</h2>

        <div className="space-y-2">
          {sources.map((source) => (
            <div
              key={source.source_id}
              onClick={() => setSelectedSource(source)}
              className={`p-3 rounded-lg cursor-pointer ${
                selectedSource?.source_id === source.source_id
                  ? "bg-blue-100"
                  : "hover:bg-gray-100"
              }`}
            >
              <div className="font-medium">{source.name}</div>
              <div className="text-sm text-gray-500">
                {source.driver_type} • {source.total_files.toLocaleString()} files
              </div>
            </div>
          ))}
        </div>

        <button
          onClick={() => setShowCreateSource(true)}
          className="mt-4 w-full rounded-md bg-blue-600 px-4 py-2 text-white"
        >
          + Add Source
        </button>
      </div>

      {/* Main content */}
      <div className="flex-1 p-6">
        {selectedSource ? (
          <SourceDetail
            source={selectedSource}
            onSync={() => handleStartSync(selectedSource.source_id)}
          />
        ) : (
          <div className="text-center text-gray-500 mt-20">
            <p>Select a source to view details</p>
          </div>
        )}
      </div>
    </div>
  );
}
```

### Job Progress View

```tsx
function JobProgressView({ jobId }: { jobId: string }) {
  const [job, setJob] = useState<IngestionJob | null>(null);
  const [events, setEvents] = useState<IngestionProgressEvent[]>([]);

  useEffect(() => {
    // Connect to WebSocket for real-time updates
    const ws = new WebSocket(`ws://${location.host}/ws/ingestion/jobs/${jobId}`);

    ws.onmessage = (e) => {
      const event = JSON.parse(e.data);
      setEvents((prev) => [...prev, event]);

      if (event.type === "progress") {
        setJob((prev) => prev ? { ...prev, ...event } : prev);
      }
    };

    return () => ws.close();
  }, [jobId]);

  const progress = job ? (job.processed_files / job.total_files) * 100 : 0;

  return (
    <div className="max-w-4xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-4">
        Ingestion Job: {jobId.slice(0, 8)}
      </h1>

      {/* Progress bar */}
      <div className="mb-6">
        <div className="flex justify-between text-sm mb-2">
          <span>{job?.status || "Loading..."}</span>
          <span>{progress.toFixed(1)}%</span>
        </div>
        <div className="h-3 bg-gray-200 rounded-full overflow-hidden">
          <div
            className="h-full bg-blue-600 transition-all duration-300"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-4 gap-4 mb-6">
        <StatCard label="Total Files" value={job?.total_files || 0} />
        <StatCard label="Processed" value={job?.processed_files || 0} />
        <StatCard label="Failed" value={job?.failed_files || 0} />
        <StatCard label="Chunks" value={job?.chunks_created || 0} />
      </div>

      {/* Event log */}
      <div className="border rounded-lg p-4 max-h-96 overflow-auto">
        <h3 className="font-semibold mb-2">Activity Log</h3>
        <div className="space-y-1 text-sm font-mono">
          {events.slice(-100).map((event, i) => (
            <div key={i} className="flex gap-2">
              <span className="text-gray-400">
                {new Date(event.timestamp).toLocaleTimeString()}
              </span>
              <span>{event.file_path || event.type}</span>
              {event.file_chunks && (
                <span className="text-green-600">({event.file_chunks} chunks)</span>
              )}
              {event.file_error && (
                <span className="text-red-600">{event.file_error}</span>
              )}
            </div>
          ))}
        </div>
      </div>

      {job?.status === "running" && (
        <button
          onClick={() => cancelJob(jobId)}
          className="mt-4 rounded-md bg-red-600 px-4 py-2 text-white"
        >
          Cancel Job
        </button>
      )}
    </div>
  );
}
```

---

## Files to Create

| File | Purpose |
|------|---------|
| `packages/futuresight-kms/python/km_labels/routers/ingestion.py` | Ingestion API: sources, jobs, bulk import |
| `packages/futuresight-kms/python/km_labels/drivers/__init__.py` | Driver registry |
| `packages/futuresight-kms/python/km_labels/drivers/base.py` | BaseDriver abstract class |
| `packages/futuresight-kms/python/km_labels/drivers/local.py` | Local filesystem driver |
| `packages/futuresight-kms/python/km_labels/drivers/github.py` | GitHub org driver |
| `packages/futuresight-kms/python/km_labels/drivers/google_drive.py` | Google Drive driver |
| `packages/futuresight-kms/python/km_labels/workers/ingestion.py` | Ingestion worker |
| `packages/futuresight-kms/python/km_labels/websocket.py` | WebSocket broadcaster |
| `orgs/mojomast/ragussy/frontend/src/pages/IngestionPage.tsx` | Ingestion dashboard UI |
| `orgs/mojomast/ragussy/frontend/src/components/JobProgress.tsx` | Job progress component |

---

## Implementation Order

1. **Database schema** - Add ingestion tables to `database.py`
2. **Base driver** - Abstract interface
3. **Local driver** - First working implementation
4. **Jobs API** - Create/list/cancel jobs
5. **WebSocket** - Progress streaming
6. **Worker** - Process jobs in background
7. **Frontend** - Ingestion dashboard
8. **Additional drivers** - GitHub, Google Drive, etc.

---

## Status

Specified 2026-04-02. Architecture defined.
