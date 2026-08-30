import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

import {
  credentialTransportKind,
} from "../../../scripts/lib/credential-transport.mjs";

const httpVerifier = readFileSync(
  new URL("../../../scripts/verify-translation-split-review.sh", import.meta.url),
  "utf8",
);
const browserTour = readFileSync(
  new URL("../../../scripts/verify-translation-split-review-tour.sh", import.meta.url),
  "utf8",
);

test("translation review credentials admit HTTPS and exact loopback HTTP", () => {
  const admitted = new Map([
    ["http://localhost:8000", "loopback-http"],
    ["HTTP://LOCALHOST:5173/translations", "loopback-http"],
    ["http://127.0.0.1:8000/", "loopback-http"],
    ["http://[::1]:5173/", "loopback-http"],
    ["https://knoxx.promethean.rest", "https"],
  ]);

  for (const [url, expected] of admitted) {
    assert.equal(credentialTransportKind(url), expected, url);
  }
});

test("translation review credentials refuse remote HTTP and URL userinfo", () => {
  const refused = [
    "",
    " http://localhost:8000",
    "http://localhost:8000\n",
    "http://knoxx.promethean.rest",
    "http://localhost@evil.example",
    "http://127.0.0.1.evil.example",
    "http://127.1:8000",
    "http://2130706433:8000",
    "http://localhost.:8000",
    "https://reviewer:secret@knoxx.promethean.rest",
    "ftp://localhost/resource",
  ];

  for (const url of refused) {
    assert.throws(() => credentialTransportKind(url), Error, url);
  }
});

test("credential-bearing verifier clients pin curl and loopback proxy policy", () => {
  assert.match(httpVerifier, /local args=\(-q -sS /);
  assert.match(httpVerifier, /health_args=\(-q -sS /);
  assert.match(httpVerifier, /args\+=\(--noproxy '\*'\)/);
  assert.match(httpVerifier, /knoxx_credential_transport_kind "\$BASE_URL"/);

  assert.match(browserTour, /health_args=\(-q -sS /);
  assert.match(browserTour, /health_args\+=\(--noproxy '\*'\)/);
  assert.match(browserTour,
    /knoxx_credential_transport_kind "\$FRONTEND_URL"/);
  assert.match(browserTour,
    /SESSION="knoxx-translation-split-review-tour-\$\{RUN_ID\}"/);
  assert.match(browserTour,
    /NO_PROXY='\*' no_proxy='\*' HTTP_PROXY='' HTTPS_PROXY='' ALL_PROXY=''/);
});
