import assert from "node:assert/strict";
import {
  chmodSync,
  existsSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import { delimiter, join } from "node:path";
import { spawnSync } from "node:child_process";
import test from "node:test";
import { fileURLToPath } from "node:url";

const backendRoot = fileURLToPath(new URL("../..", import.meta.url));
const smokeScript = "scripts/smoke_test.sh";
const bootstrapVerifier = fileURLToPath(
  new URL("../../../scripts/verify-bootstrap-credential-rotation.sh", import.meta.url),
);

function fakeCurlDir() {
  const dir = mkdtempSync(join(tmpdir(), "knoxx-smoke-curl-"));
  const curl = join(dir, "curl");
  writeFileSync(
    curl,
    `#!/usr/bin/env bash
set -euo pipefail
out=""
route="proxyable"
headers=()
first_arg="\${1:-}"
while [[ $# -gt 0 ]]; do
  if [[ "$1" == "-o" ]]; then
    out="$2"
    shift 2
  elif [[ "$1" == "-H" ]]; then
    headers+=("$2")
    shift 2
  elif [[ "$1" == "--noproxy" && "\${2:-}" == "*" ]]; then
    route="direct"
    shift 2
  else
    shift
  fi
done
header_list="$(IFS='|'; printf '%s' "\${headers[*]}")"
[[ -z "\${FAKE_CURL_CALL_FILE:-}" ]] || printf '%s\t%s\t%s\n' "$first_arg" "$route" "$header_list" >> "$FAKE_CURL_CALL_FILE"
[[ -z "$out" ]] || printf '{}\n' > "$out"
printf '200'
`,
  );
  chmodSync(curl, 0o755);
  return dir;
}

function runSmoke({ baseUrl, apiKey = "", bearer = "" }) {
  const curlDir = fakeCurlDir();
  const callFile = join(curlDir, "calls");
  try {
    const result = spawnSync("bash", [smokeScript], {
      cwd: backendRoot,
      encoding: "utf8",
      env: {
        ...process.env,
        PATH: `${curlDir}${delimiter}${process.env.PATH ?? ""}`,
        FAKE_CURL_CALL_FILE: callFile,
        BASE_URL: baseUrl,
        KNOXX_API_KEY: apiKey,
        MODEL_LAB_OPENAI_API_KEY: bearer,
      },
    });
    const curlInvocations = existsSync(callFile)
      ? readFileSync(callFile, "utf8")
          .trim()
          .split("\n")
          .map((line) => {
            const [firstArg, route, headerList = ""] = line.split("\t");
            return {
              firstArg,
              route,
              headers: headerList === "" ? [] : headerList.split("|"),
            };
          })
      : [];
    return Object.assign(result, {
      curlCalls: curlInvocations.length,
      curlRoutes: curlInvocations.map(({ route }) => route),
      curlFirstArgs: curlInvocations.map(({ firstArg }) => firstArg),
      curlHeaders: curlInvocations.map(({ headers }) => headers),
    });
  } finally {
    rmSync(curlDir, { recursive: true, force: true });
  }
}

test("rejects bearer credentials over remote HTTP before invoking curl", () => {
  const secret = "must-not-appear";
  const result = runSmoke({ baseUrl: "http://example.com", bearer: secret });
  assert.notEqual(result.status, 0);
  assert.equal(result.curlCalls, 0);
  assert.match(result.stderr, /Refusing to send Knoxx smoke-test credentials/);
  assert.doesNotMatch(`${result.stdout}${result.stderr}`, new RegExp(secret));
});

test("rejects API-key credentials over crafted loopback userinfo URLs", () => {
  const result = runSmoke({
    baseUrl: "http://127.0.0.1@remote.example",
    apiKey: "must-not-appear",
  });
  assert.notEqual(result.status, 0);
  assert.equal(result.curlCalls, 0);
  assert.match(result.stderr, /Refusing to send Knoxx smoke-test credentials/);
});

test("allows credentialed HTTPS and exact loopback HTTP URLs", () => {
  for (const baseUrl of [
    "https://knoxx.example",
    "http://localhost:8000",
    "http://127.0.0.1:8000",
    "http://[::1]:8000",
  ]) {
    const result = runSmoke({ baseUrl, bearer: "test-only" });
    assert.equal(result.status, 0, `${baseUrl}: ${result.stderr}`);
    assert.ok(result.curlCalls > 0, `${baseUrl}: curl was not exercised`);
    assert.ok(
      result.curlFirstArgs.every((arg) => arg === "-q"),
      `${baseUrl}: curlrc disabling was not the first argument`,
    );
    if (baseUrl.startsWith("http://")) {
      assert.ok(
        result.curlRoutes.every((route) => route === "direct"),
        `${baseUrl}: loopback traffic remained proxyable`,
      );
    }
  }
});

test("keeps API and bearer credentials in their distinct headers", () => {
  for (const credentials of [
    { apiKey: "api-only", bearer: "" },
    { apiKey: "", bearer: "bearer-only" },
    { apiKey: "api-both", bearer: "bearer-both" },
  ]) {
    const result = runSmoke({
      baseUrl: "https://knoxx.example",
      ...credentials,
    });
    assert.equal(result.status, 0, result.stderr);
    assert.ok(result.curlHeaders.length > 0);
    for (const headers of result.curlHeaders) {
      assert.deepEqual(
        headers.filter((header) => header.startsWith("X-API-Key: ")),
        credentials.apiKey ? [`X-API-Key: ${credentials.apiKey}`] : [],
      );
      assert.deepEqual(
        headers.filter((header) => header.startsWith("Authorization: ")),
        credentials.bearer
          ? [`Authorization: Bearer ${credentials.bearer}`]
          : [],
      );
    }
  }
});

test("bootstrap verifier disables curl config and proxies for every request", () => {
  const curlInvocations = readFileSync(bootstrapVerifier, "utf8")
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => /^(?:if )?curl /.test(line));

  assert.equal(curlInvocations.length, 3);
  assert.ok(
    curlInvocations.every((line) => /^(?:if )?curl -q --noproxy '\*' /.test(line)),
    `unhardened curl invocation:\n${curlInvocations.join("\n")}`,
  );
});

test("bootstrap verifier tracks the Node server process directly", () => {
  const source = readFileSync(bootstrapVerifier, "utf8");
  const startServer = source.slice(
    source.indexOf("start_server()"),
    source.indexOf("wait_for_ready()"),
  );

  assert.match(startServer, /^\s+exec env \\$/m);
  assert.doesNotMatch(startServer, /^\s+env \\$/m);
  assert.match(startServer, /^\s+node "\$SERVER_ENTRY"$/m);
});

test("bootstrap verifier builds and runs only inside trapped evidence", () => {
  const source = readFileSync(bootstrapVerifier, "utf8");

  assert.match(source, /^BUILD_ROOT="\$\{EVIDENCE_DIR\}\/reviewed-checkout"$/m);
  assert.match(source, /^SERVER_ENTRY="\$\{BUILD_ROOT\}\/backend\/dist\/server\.js"$/m);
  assert.match(
    source,
    /git -C "\$REPO_ROOT" archive "\$REVIEWED_HEAD" -- backend contracts shared/,
  );
  assert.match(source, /pnpm -C "\$BUILD_ROOT\/backend" build/);
  assert.doesNotMatch(source, /pnpm -C "\$REPO_ROOT\/backend" build/);

  const cleanup = source.slice(
    source.indexOf("cleanup()"),
    source.indexOf("trap cleanup EXIT"),
  );
  assert.match(cleanup, /rm -rf "\$EVIDENCE_DIR"/);
});
