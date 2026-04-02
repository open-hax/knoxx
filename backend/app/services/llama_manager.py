from __future__ import annotations

import asyncio
import shlex
from asyncio.subprocess import Process
from datetime import UTC, datetime
from pathlib import Path

import httpx

from app.core.config import Settings
from app.core.schemas import ServerStartRequest, ServerStatusResponse
from app.services.event_bus import EventBus


class LlamaServerManager:
    def __init__(self, settings: Settings, event_bus: EventBus) -> None:
        self._settings = settings
        self._event_bus = event_bus
        self._process: Process | None = None
        self._stdout_task: asyncio.Task[None] | None = None
        self._stderr_task: asyncio.Task[None] | None = None
        self._lock = asyncio.Lock()
        self._command: list[str] = []
        self._started_at: datetime | None = None
        self._model_path: str | None = None
        self._active_port: int = settings.llama_port

    @property
    def base_url(self) -> str:
        return f"http://{self._settings.llama_host}:{self._active_port}"

    @property
    def pid(self) -> int | None:
        if self._process and self._process.returncode is None:
            return self._process.pid
        return None

    async def start(self, req: ServerStartRequest) -> ServerStatusResponse:
        async with self._lock:
            if self._process and self._process.returncode is None:
                if not req.multi_instance_mode and self._model_path == str(Path(req.model_path).resolve()):
                    return self.status()
                await self._stop_locked()

            model_file = Path(req.model_path).resolve()
            if not model_file.exists() or not model_file.is_file():
                raise FileNotFoundError(f"Model not found: {model_file}")
            model_path = str(model_file)
            port = req.port if req.port is not None else self._settings.llama_port
            command = [
                self._settings.llama_server_path or self._settings.llama_server_bin,
                "--host",
                self._settings.llama_host,
                "--port",
                str(port),
                "-m",
                model_path,
                "--ctx-size",
                str(req.ctx_size if req.ctx_size is not None else self._settings.default_ctx),
                "--n-gpu-layers",
                str(req.gpu_layers if req.gpu_layers is not None else self._settings.default_gpu_layers),
            ]

            if req.threads is not None:
                command.extend(["-t", str(req.threads)])
            else:
                command.extend(["-t", str(self._settings.default_threads)])
            if req.batch_size is not None:
                command.extend(["-b", str(req.batch_size)])
            if req.ubatch_size is not None:
                command.extend(["-ub", str(req.ubatch_size)])
            if req.flash_attention is not None:
                command.extend(["--flash-attn", "on" if req.flash_attention else "off"])
            if req.mmap:
                command.append("--mmap")
            if req.mlock:
                command.append("--mlock")

            if self._settings.llama_extra_args.strip():
                command.extend(shlex.split(self._settings.llama_extra_args))
            if req.extra_args:
                command.extend(req.extra_args)

            self._process = await asyncio.create_subprocess_exec(
                *command,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
            )
            self._command = command
            self._started_at = datetime.now(UTC)
            self._model_path = model_path
            self._active_port = port

            self._stdout_task = asyncio.create_task(self._pump_pipe("stdout", self._process.stdout))
            self._stderr_task = asyncio.create_task(self._pump_pipe("stderr", self._process.stderr))

            await self._event_bus.publish(
                "events",
                {"event": "server_started", "pid": self._process.pid, "model_path": model_path},
            )

            return self.status()

    async def stop(self) -> ServerStatusResponse:
        async with self._lock:
            await self._stop_locked()
            return self.status()

    async def restart(self, req: ServerStartRequest) -> ServerStatusResponse:
        await self.stop()
        return await self.start(req)

    async def warmup(self, prompt: str = "Hello", max_tokens: int = 8) -> tuple[float, str | None]:
        started = asyncio.get_running_loop().time()
        payload = {
            "model": "",
            "messages": [{"role": "user", "content": prompt}],
            "max_tokens": max_tokens,
            "stream": False,
        }

        timeout = httpx.Timeout(self._settings.local_request_timeout_seconds)
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.post(f"{self.base_url}/v1/chat/completions", json=payload)
            response.raise_for_status()
            body = response.json()
            model = body.get("model")

        latency_ms = (asyncio.get_running_loop().time() - started) * 1000.0
        return latency_ms, model

    def status(self) -> ServerStatusResponse:
        running = self._process is not None and self._process.returncode is None
        return ServerStatusResponse(
            running=running,
            pid=self._process.pid if running and self._process else None,
            model_path=self._model_path if running else None,
            host=self._settings.llama_host,
            port=self._active_port,
            started_at=self._started_at if running else None,
            command=self._command if running else [],
        )

    async def _stop_locked(self) -> None:
        if not self._process or self._process.returncode is not None:
            self._clear_process_state()
            return

        self._process.terminate()
        try:
            await asyncio.wait_for(self._process.wait(), timeout=8)
        except TimeoutError:
            self._process.kill()
            await self._process.wait()

        await self._event_bus.publish("events", {"event": "server_stopped", "pid": self._process.pid})

        if self._stdout_task:
            self._stdout_task.cancel()
        if self._stderr_task:
            self._stderr_task.cancel()
        self._clear_process_state()

    def _clear_process_state(self) -> None:
        self._process = None
        self._stdout_task = None
        self._stderr_task = None
        self._command = []
        self._started_at = None
        self._model_path = None
        self._active_port = self._settings.llama_port

    async def _pump_pipe(self, stream: str, pipe: asyncio.StreamReader | None) -> None:
        if pipe is None:
            return

        while True:
            line = await pipe.readline()
            if not line:
                break
            text = line.decode("utf-8", errors="replace").rstrip()
            if not text:
                continue
            await self._event_bus.publish("console", {"stream": stream, "line": text})
