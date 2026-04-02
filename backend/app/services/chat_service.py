from __future__ import annotations

import asyncio
import json
from dataclasses import dataclass
from time import perf_counter
from typing import Any
from uuid import uuid4

import httpx

from app.core.config import Settings
from app.core.schemas import ChatRequest
from app.services.event_bus import EventBus
from app.services.llama_manager import LlamaServerManager
from app.services.metrics import MetricsSampler
from app.services.run_store import RunStore


@dataclass(slots=True)
class RunMetrics:
    ttft_ms: float | None = None
    total_time_ms: float | None = None
    input_tokens: int | None = None
    output_tokens: int | None = None
    tokens_per_s: float | None = None


class ChatService:
    def __init__(
        self,
        settings: Settings,
        event_bus: EventBus,
        llama_manager: LlamaServerManager,
        run_store: RunStore,
        metrics_sampler: MetricsSampler,
    ) -> None:
        self._settings = settings
        self._event_bus = event_bus
        self._llama_manager = llama_manager
        self._run_store = run_store
        self._metrics_sampler = metrics_sampler
        self._tasks: set[asyncio.Task[None]] = set()

    def active_run_count(self) -> int:
        return sum(1 for t in self._tasks if not t.done())

    async def enqueue_chat(self, req: ChatRequest) -> str:
        run_id = uuid4().hex
        session_id = str(req.metadata.get("session_id", "")) if req.metadata else ""
        messages = [msg.model_dump() for msg in req.messages]
        settings = {
            "temperature": req.temperature,
            "top_p": req.top_p,
            "top_k": req.top_k,
            "min_p": req.min_p,
            "repeat_penalty": req.repeat_penalty,
            "presence_penalty": req.presence_penalty,
            "frequency_penalty": req.frequency_penalty,
            "seed": req.seed,
            "stop": req.stop,
            "max_tokens": req.max_tokens,
            "stream": True,
            "metadata": req.metadata,
        }
        if req.system_prompt:
            settings["system_prompt"] = req.system_prompt
        self._run_store.create_run(
            run_id=run_id,
            model=req.model,
            request_messages=messages,
            settings=settings,
            resources=self._metrics_sampler.snapshot(),
        )
        await self._event_bus.publish(
            "events",
            {"event": "run_queued", "run_id": run_id, "session_id": session_id},
        )

        task = asyncio.create_task(self._execute_run(run_id, req, session_id=session_id))
        self._tasks.add(task)
        task.add_done_callback(self._tasks.discard)
        return run_id

    async def _execute_run(self, run_id: str, req: ChatRequest, session_id: str = "") -> None:
        await self._event_bus.publish(
            "events",
            {"event": "run_started", "run_id": run_id, "session_id": session_id},
        )
        self._run_store.append_event(run_id, "status", {"status": "running"})

        started = perf_counter()
        first_token_time: float | None = None
        output_tokens = 0
        input_tokens: int | None = None
        output_token_usage: int | None = None

        messages = [msg.model_dump(exclude_none=True) for msg in req.messages]
        if req.system_prompt and not any(m.get("role") == "system" for m in messages):
            messages.insert(0, {"role": "system", "content": req.system_prompt})

        payload: dict[str, Any] = {
            "model": req.model or "",
            "messages": messages,
            "temperature": req.temperature,
            "top_p": req.top_p,
            "top_k": req.top_k,
            "min_p": req.min_p,
            "repeat_penalty": req.repeat_penalty,
            "presence_penalty": req.presence_penalty,
            "frequency_penalty": req.frequency_penalty,
            "seed": req.seed,
            "max_tokens": req.max_tokens,
            "stop": req.stop,
            "stream": True,
        }
        payload = {k: v for k, v in payload.items() if v is not None}

        timeout = httpx.Timeout(self._settings.local_request_timeout_seconds)
        stream_ok = False
        error_message: str | None = None
        stop_event = asyncio.Event()
        resource_samples: list[dict[str, Any]] = []
        sample_task = asyncio.create_task(self._collect_run_samples(run_id, resource_samples, stop_event))

        try:
            async with httpx.AsyncClient(timeout=timeout) as client:
                stream_ok, first_token_time, output_tokens, input_tokens, output_token_usage = await self._try_stream_completion(
                    client=client,
                    run_id=run_id,
                    payload=payload,
                    started=started,
                    session_id=session_id,
                )
                if not stream_ok:
                    first_token_time, output_tokens, input_tokens, output_token_usage = await self._fallback_non_stream(
                        client=client,
                        run_id=run_id,
                        payload=payload,
                        started=started,
                        session_id=session_id,
                    )
        except Exception as exc:
            error_message = str(exc)
        finally:
            stop_event.set()
            await sample_task

        total_time_ms = (perf_counter() - started) * 1000.0
        ttft_ms = None if first_token_time is None else (first_token_time - started) * 1000.0
        final_output_tokens = output_token_usage if output_token_usage is not None else output_tokens
        tokens_per_s = None
        if total_time_ms > 0 and final_output_tokens:
            tokens_per_s = final_output_tokens / (total_time_ms / 1000.0)

        metrics = RunMetrics(
            ttft_ms=ttft_ms,
            total_time_ms=total_time_ms,
            input_tokens=input_tokens,
            output_tokens=final_output_tokens,
            tokens_per_s=tokens_per_s,
        )

        resource_summary = self._summarize_resources(resource_samples)

        if error_message:
            self._run_store.finalize_run(
                run_id=run_id,
                status="failed",
                ttft_ms=metrics.ttft_ms,
                total_time_ms=metrics.total_time_ms,
                input_tokens=metrics.input_tokens,
                output_tokens=metrics.output_tokens,
                tokens_per_s=metrics.tokens_per_s,
                resources=resource_summary,
                error=error_message,
            )
            await self._event_bus.publish(
                "events",
                {"event": "run_failed", "run_id": run_id, "error": error_message, "session_id": session_id},
            )
            await self._event_bus.publish(
                "events",
                {"event": "run_finished", "run_id": run_id, "status": "failed", "session_id": session_id},
            )
            return

        self._run_store.finalize_run(
            run_id=run_id,
            status="completed",
            ttft_ms=metrics.ttft_ms,
            total_time_ms=metrics.total_time_ms,
            input_tokens=metrics.input_tokens,
            output_tokens=metrics.output_tokens,
            tokens_per_s=metrics.tokens_per_s,
            resources=resource_summary,
        )
        await self._event_bus.publish(
            "events",
            {"event": "run_completed", "run_id": run_id, "session_id": session_id},
        )
        await self._event_bus.publish(
            "events",
            {
                "event": "run_finished",
                "run_id": run_id,
                "status": "completed",
                "session_id": session_id,
                "input_tokens": metrics.input_tokens,
                "output_tokens": metrics.output_tokens,
            },
        )

    async def _try_stream_completion(
        self,
        client: httpx.AsyncClient,
        run_id: str,
        payload: dict[str, Any],
        started: float,
        session_id: str = "",
    ) -> tuple[bool, float | None, int, int | None, int | None]:
        first_token_time: float | None = None
        output_tokens = 0
        prompt_tokens: int | None = None
        completion_tokens: int | None = None

        async with client.stream("POST", f"{self._llama_manager.base_url}/v1/chat/completions", json=payload) as resp:
            if resp.status_code >= 400:
                return False, None, 0, None, None

            async for chunk in self._iter_sse_json(resp):
                usage = chunk.get("usage") or {}
                prompt_tokens = usage.get("prompt_tokens", prompt_tokens)
                completion_tokens = usage.get("completion_tokens", completion_tokens)

                choices = chunk.get("choices") or []
                if not choices:
                    continue

                choice0 = choices[0] or {}
                delta = choice0.get("delta") or {}

                token = None
                if isinstance(delta, dict):
                    token = delta.get("content")
                    if token is None:
                        token = delta.get("reasoning_content")
                if token is None:
                    token = choice0.get("text")
                if token is None:
                    message = choice0.get("message") or {}
                    if isinstance(message, dict):
                        token = message.get("content")
                        if token is None:
                            token = message.get("reasoning_content")
                if token is None:
                    token = chunk.get("content")
                if token is None:
                    token = chunk.get("reasoning_content")

                if token:
                    token = str(token)
                    output_tokens += 1
                    if first_token_time is None:
                        first_token_time = perf_counter()
                    await self._event_bus.publish(
                        "tokens",
                        {
                            "run_id": run_id,
                            "session_id": session_id,
                            "index": output_tokens,
                            "token": token,
                            "elapsed_ms": (perf_counter() - started) * 1000.0,
                        },
                    )
                    self._run_store.append_event(run_id, "token", {"index": output_tokens, "token": token})

        if output_tokens == 0:
            self._run_store.append_event(
                run_id,
                "stream_done",
                {"ok": True, "note": "no tokens parsed from stream; using non-stream fallback"},
            )
            first_token_time, output_tokens, prompt_tokens_fallback, completion_tokens_fallback = await self._fallback_non_stream(
                client=client,
                run_id=run_id,
                payload=payload,
                started=started,
                session_id=session_id,
            )
            if prompt_tokens is None:
                prompt_tokens = prompt_tokens_fallback
            if completion_tokens is None:
                completion_tokens = completion_tokens_fallback
            return True, first_token_time, output_tokens, prompt_tokens, completion_tokens

        self._run_store.append_event(run_id, "stream_done", {"ok": True})
        return True, first_token_time, output_tokens, prompt_tokens, completion_tokens

    async def _fallback_non_stream(
        self,
        client: httpx.AsyncClient,
        run_id: str,
        payload: dict[str, Any],
        started: float,
        session_id: str = "",
    ) -> tuple[float | None, int, int | None, int | None]:
        payload = dict(payload)
        payload["stream"] = False

        response = await client.post(f"{self._llama_manager.base_url}/v1/chat/completions", json=payload)
        response.raise_for_status()
        body = response.json()

        usage = body.get("usage") or {}
        prompt_tokens = usage.get("prompt_tokens")
        completion_tokens = usage.get("completion_tokens")

        choices = body.get("choices") or []
        content = ""
        if choices:
            message = choices[0].get("message") or {}
            content = message.get("content") or message.get("reasoning_content") or ""

        first_token_time = perf_counter()
        output_tokens = 1 if content else 0

        if content:
            await self._event_bus.publish(
                "tokens",
                {
                    "run_id": run_id,
                    "session_id": session_id,
                    "index": 1,
                    "token": content,
                    "elapsed_ms": (perf_counter() - started) * 1000.0,
                    "fallback": True,
                },
            )
            self._run_store.append_event(run_id, "token", {"index": 1, "token": content, "fallback": True})
        self._run_store.append_event(run_id, "stream_done", {"ok": True, "fallback": True})

        return first_token_time, output_tokens, prompt_tokens, completion_tokens

    async def _iter_sse_json(self, response: httpx.Response):
        buffer = b""
        async for chunk in response.aiter_bytes():
            if not chunk:
                continue
            buffer += chunk.replace(b"\r\n", b"\n")

            while b"\n\n" in buffer:
                event_raw, buffer = buffer.split(b"\n\n", 1)
                data_lines: list[str] = []
                for line in event_raw.decode("utf-8", errors="replace").split("\n"):
                    if not line or line.startswith(":"):
                        continue
                    if line.startswith("data:"):
                        data_lines.append(line[5:].lstrip())

                if not data_lines:
                    continue
                payload = "\n".join(data_lines).strip()
                if payload == "[DONE]":
                    return

                try:
                    parsed = json.loads(payload)
                except json.JSONDecodeError:
                    await self._event_bus.publish("console", {"stream": "sse", "line": f"Unparsable SSE data: {payload}"})
                    continue

                yield parsed

    async def _collect_run_samples(
        self,
        run_id: str,
        samples: list[dict[str, Any]],
        stop_event: asyncio.Event,
    ) -> None:
        while not stop_event.is_set():
            snap = self._metrics_sampler.snapshot()
            samples.append(snap)
            self._run_store.append_event(run_id, "stats", snap)
            await asyncio.sleep(1.0)

    @staticmethod
    def _summarize_resources(samples: list[dict[str, Any]]) -> dict[str, Any]:
        if not samples:
            return {}

        def values(key: str) -> list[float]:
            vals: list[float] = []
            for item in samples:
                value = item.get(key)
                if isinstance(value, (int, float)):
                    vals.append(float(value))
            return vals

        cpu_vals = values("cpu_percent")
        mem_vals = values("memory_percent")
        rss_vals = [
            float(item.get("llama", {}).get("rss_bytes"))
            for item in samples
            if isinstance(item.get("llama", {}).get("rss_bytes"), (int, float))
        ]

        gpu_mem_vals: list[float] = []
        gpu_util_vals: list[float] = []
        for item in samples:
            gpu = item.get("gpu")
            if isinstance(gpu, list):
                for dev in gpu:
                    used = dev.get("memory_used_bytes")
                    util = dev.get("util_gpu")
                    if isinstance(used, (int, float)):
                        gpu_mem_vals.append(float(used))
                    if isinstance(util, (int, float)):
                        gpu_util_vals.append(float(util))

        return {
            "samples": len(samples),
            "cpu_percent_avg": (sum(cpu_vals) / len(cpu_vals)) if cpu_vals else None,
            "cpu_percent_max": max(cpu_vals) if cpu_vals else None,
            "memory_percent_avg": (sum(mem_vals) / len(mem_vals)) if mem_vals else None,
            "memory_percent_max": max(mem_vals) if mem_vals else None,
            "llama_rss_max_bytes": max(rss_vals) if rss_vals else None,
            "gpu_memory_max_bytes": max(gpu_mem_vals) if gpu_mem_vals else None,
            "gpu_util_max": max(gpu_util_vals) if gpu_util_vals else None,
        }
