from __future__ import annotations

import asyncio
import contextlib
import importlib
from datetime import UTC, datetime
from typing import Any

import psutil

from app.core.config import Settings
from app.services.event_bus import EventBus
from app.services.llama_manager import LlamaServerManager

pynvml = None
try:
    pynvml = importlib.import_module("pynvml")
    HAS_NVML = True
except Exception:
    HAS_NVML = False


class MetricsSampler:
    def __init__(self, settings: Settings, event_bus: EventBus, llama_manager: LlamaServerManager) -> None:
        self._settings = settings
        self._event_bus = event_bus
        self._llama_manager = llama_manager
        self._task: asyncio.Task[None] | None = None
        self._last_snapshot: dict[str, Any] = {}
        self._nvml_ready = False
        self._active_clients_provider = lambda: 0
        self._active_runs_provider = lambda: 0
        self._last_network_ts: float | None = None
        self._last_network_rx_bytes: int | None = None
        self._last_network_tx_bytes: int | None = None

    def set_activity_providers(self, active_clients_provider, active_runs_provider) -> None:
        self._active_clients_provider = active_clients_provider
        self._active_runs_provider = active_runs_provider

    async def start(self) -> None:
        if self._task and not self._task.done():
            return
        if HAS_NVML:
            try:
                assert pynvml is not None
                pynvml.nvmlInit()
                self._nvml_ready = True
            except Exception:
                self._nvml_ready = False
        self._task = asyncio.create_task(self._run())

    async def stop(self) -> None:
        if self._task:
            self._task.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await self._task
        self._task = None
        if self._nvml_ready:
            with contextlib.suppress(Exception):
                assert pynvml is not None
                pynvml.nvmlShutdown()
            self._nvml_ready = False

    def snapshot(self) -> dict[str, Any]:
        return dict(self._last_snapshot)

    async def _run(self) -> None:
        while True:
            snap = self._collect_snapshot()
            self._last_snapshot = snap
            await self._event_bus.publish("stats", snap)
            await asyncio.sleep(self._settings.metrics_interval_seconds)

    def _collect_snapshot(self) -> dict[str, Any]:
        now = datetime.now(UTC).isoformat()
        vm = psutil.virtual_memory()
        net = psutil.net_io_counters()
        stats: dict[str, Any] = {
            "timestamp": now,
            "cpu_percent": psutil.cpu_percent(interval=None),
            "memory_percent": vm.percent,
            "memory_used_bytes": vm.used,
            "memory_total_bytes": vm.total,
            "memory_available_bytes": vm.available,
            "network": {
                "bytes_sent_total": int(net.bytes_sent),
                "bytes_recv_total": int(net.bytes_recv),
            },
        }

        now_monotonic = asyncio.get_running_loop().time()
        if (
            self._last_network_ts is not None
            and self._last_network_rx_bytes is not None
            and self._last_network_tx_bytes is not None
        ):
            elapsed = max(0.001, now_monotonic - self._last_network_ts)
            rx_bps = max(0, int((int(net.bytes_recv) - self._last_network_rx_bytes) / elapsed))
            tx_bps = max(0, int((int(net.bytes_sent) - self._last_network_tx_bytes) / elapsed))
            stats["network"]["rx_bytes_per_sec"] = rx_bps
            stats["network"]["tx_bytes_per_sec"] = tx_bps
            stats["network"]["total_bytes_per_sec"] = rx_bps + tx_bps

        self._last_network_ts = now_monotonic
        self._last_network_rx_bytes = int(net.bytes_recv)
        self._last_network_tx_bytes = int(net.bytes_sent)

        active_clients = max(1, int(self._active_clients_provider() or 1))
        active_runs = int(self._active_runs_provider() or 0)
        stats["multi_user"] = {
            "active_clients": active_clients,
            "active_runs": active_runs,
            "memory_available_per_client_bytes": int(vm.available / active_clients),
        }

        pid = self._llama_manager.pid
        if pid:
            try:
                proc = psutil.Process(pid)
                mem = proc.memory_info()
                stats["llama"] = {
                    "pid": pid,
                    "cpu_percent": proc.cpu_percent(interval=None),
                    "rss_bytes": mem.rss,
                    "vms_bytes": mem.vms,
                }
            except Exception:
                stats["llama"] = {"pid": pid, "alive": False}

        gpu = self._collect_gpu()
        if gpu:
            stats["gpu"] = gpu
            total_free_gpu = 0
            for dev in gpu:
                total = int(dev.get("memory_total_bytes", 0) or 0)
                used = int(dev.get("memory_used_bytes", 0) or 0)
                total_free_gpu += max(0, total - used)
            stats["multi_user"]["gpu_memory_available_total_bytes"] = total_free_gpu
            stats["multi_user"]["gpu_memory_available_per_client_bytes"] = int(total_free_gpu / active_clients)
        return stats

    def _collect_gpu(self) -> list[dict[str, Any]]:
        if not self._nvml_ready:
            return []

        devices: list[dict[str, Any]] = []
        try:
            assert pynvml is not None
            count = pynvml.nvmlDeviceGetCount()
            for idx in range(count):
                handle = pynvml.nvmlDeviceGetHandleByIndex(idx)
                name = pynvml.nvmlDeviceGetName(handle)
                mem = pynvml.nvmlDeviceGetMemoryInfo(handle)
                util = pynvml.nvmlDeviceGetUtilizationRates(handle)
                devices.append(
                    {
                        "index": idx,
                        "name": name.decode("utf-8") if isinstance(name, bytes) else str(name),
                        "util_gpu": util.gpu,
                        "util_mem": util.memory,
                        "memory_used_bytes": mem.used,
                        "memory_total_bytes": mem.total,
                    }
                )
        except Exception:
            return []
        return devices
