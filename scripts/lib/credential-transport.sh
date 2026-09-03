#!/usr/bin/env bash
# Credential-bearing verifier URL admission. Sourcing scripts decide how to
# report refusal; this helper deliberately never prints the rejected URL.

# shellcheck shell=bash

knoxx_credential_transport_kind() {
  local raw_url="$1" helper_dir
  helper_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  printf '%s' "$raw_url" | node "${helper_dir}/credential-transport.mjs"
}
