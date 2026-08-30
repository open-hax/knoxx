import fs from "node:fs";
import { pathToFileURL } from "node:url";

const LOOPBACK_HOSTS = new Set(["localhost", "127.0.0.1", "[::1]"]);

function hasExactLoopbackAuthority(raw) {
  const authority = /^http:\/\/([^/?#]+)(?:[/?#]|$)/i.exec(raw)?.[1] ?? "";
  return /^(?:localhost|127\.0\.0\.1)(?::[0-9]+)?$/i.test(authority)
    || /^\[::1\](?::[0-9]+)?$/i.test(authority);
}

export function credentialTransportKind(raw) {
  if (typeof raw !== "string" || raw.length === 0 || raw.trim() !== raw) {
    throw new Error("base URL must be a nonblank URL without surrounding whitespace");
  }

  let parsed;
  try {
    parsed = new URL(raw);
  } catch {
    throw new Error("base URL is not a valid URL");
  }

  if (parsed.username !== "" || parsed.password !== "") {
    throw new Error("base URL userinfo is forbidden");
  }
  if (parsed.protocol === "https:" && parsed.hostname !== "") {
    return "https";
  }
  if (parsed.protocol === "http:"
      && LOOPBACK_HOSTS.has(parsed.hostname)
      && hasExactLoopbackAuthority(raw)) {
    return "loopback-http";
  }
  throw new Error("credential transport requires HTTPS or an exact loopback HTTP host");
}

const invokedDirectly = process.argv[1]
  && import.meta.url === pathToFileURL(process.argv[1]).href;

if (invokedDirectly) {
  try {
    process.stdout.write(credentialTransportKind(fs.readFileSync(0, "utf8")));
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 2;
  }
}
