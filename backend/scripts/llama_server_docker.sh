#!/usr/bin/env bash
set -euo pipefail

# Run llama-server inside NVIDIA CUDA container using host network.
# We replace '--host 127.0.0.1' with '--host 0.0.0.0' for container binding.
args=()
while [[ $# -gt 0 ]]; do
  if [[ "$1" == "--host" ]] && [[ $# -ge 2 ]]; then
    args+=("--host" "0.0.0.0")
    shift 2
    continue
  fi
  args+=("$1")
  shift
 done

exec sudo -n docker run --gpus all --rm --network host \
  -v /home/mojo:/home/mojo \
  nvcr.io/nvidia/cuda:13.1.0-devel-ubuntu22.04 \
  /bin/bash -lc 'export LD_LIBRARY_PATH="/home/mojo/llama.cpp/build/bin:/usr/local/cuda/lib64:$LD_LIBRARY_PATH"; exec /home/mojo/llama.cpp/build/bin/llama-server "$@"' _ "${args[@]}"
