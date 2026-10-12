// SPDX-License-Identifier: GPL-3.0-or-later
// Prospective OpenCode custom-tool boundary; qualification is pending.
import https from "node:https";
import { createHash } from "node:crypto";

const HOSTS = new Set(["api.github.com", "raw.githubusercontent.com", "github.com", "opencode.ai"]);
const MAX_BYTES = 5 * 1024 * 1024;
function checked(raw) {
  if (typeof raw !== "string" || Buffer.byteLength(raw, "utf8") > 1024 || /[\u0000-\u0020\u007f]/u.test(raw)) throw new Error("PUBLIC_FETCH_URL_REFUSED");
  const url = new URL(raw);
  if (url.protocol !== "https:" || !HOSTS.has(url.hostname) || (url.port && url.port !== "443")
      || url.username || url.password || url.hash || url.href.length > 1024) throw new Error("PUBLIC_FETCH_URL_REFUSED");
  return url;
}
function response(url, agent, signal) {
  return new Promise((resolve, reject) => {
    const request = https.request(url, {
      method: "GET", agent, signal, rejectUnauthorized: true,
      headers: { Accept: "application/json,text/plain,text/html;q=0.8", "Accept-Encoding": "identity", "User-Agent": "knoxx-native-public-assessment" },
    }, resolve);
    request.on("error", reject);
    request.end();
  });
}

export default {
  description: "Read an untrusted public HTTPS response page from exactly four approved hosts. No tokens, cookies, filesystem or shell. Compare response SHA across pages; disclose all refusals and incomplete reading.",
  // Existing v1.19.0 registry compatibility surface; execute validates too.
  args: { url: { type: "string", description: "Exact public HTTPS URL, preferably immutable source at the supplied head." }, offset: { type: "integer", minimum: 0, description: "Unicode-codepoint offset; initially 0, then next_offset." } },
  async execute(args, context) {
    if (!args || typeof args !== "object" || Object.keys(args).some((k) => k !== "url" && k !== "offset")
        || !Number.isSafeInteger(args.offset) || args.offset < 0) throw new Error("PUBLIC_FETCH_ARGS_REFUSED");
    let url = checked(args.url);
    if (typeof context?.ask !== "function") throw new Error("PUBLIC_FETCH_PERMISSION_CONTEXT_REQUIRED");
    await context.ask({ permission: "native_public_fetch", patterns: [url.href], always: [], metadata: { url: url.href } });
    const controller = new AbortController();
    const cancel = () => controller.abort();
    if (context.abort?.aborted) controller.abort();
    else context.abort?.addEventListener("abort", cancel, { once: true });
    const timer = setTimeout(cancel, 30_000);
    const agent = new https.Agent({ keepAlive: false, maxSockets: 1 });
    const redirects = [];
    let active;
    try {
      for (let hop = 0; ; hop += 1) {
        active = await response(url, agent, controller.signal);
        const status = active.statusCode;
        if ([301, 302, 303, 307, 308].includes(status)) {
          if (hop >= 3 || typeof active.headers.location !== "string") throw new Error("PUBLIC_FETCH_REDIRECT_REFUSED");
          const next = checked(new URL(active.headers.location, url).href);
          redirects.push({ from: url.href, to: next.href, status });
          active.destroy(); active = undefined; url = next;
          continue;
        }
        if (status !== 200) throw new Error("PUBLIC_FETCH_HTTP_" + status);
        const encoding = active.headers["content-encoding"];
        if (encoding && encoding !== "identity") throw new Error("PUBLIC_FETCH_ENCODING_REFUSED");
        const length = active.headers["content-length"];
        if (length && (!/^[0-9]+$/u.test(length) || Number(length) > MAX_BYTES)) throw new Error("PUBLIC_FETCH_SIZE_REFUSED");
        const chunks = []; let size = 0;
        for await (const chunk of active) {
          size += chunk.length;
          if (size > MAX_BYTES) throw new Error("PUBLIC_FETCH_SIZE_REFUSED");
          chunks.push(chunk);
        }
        if (!active.complete || (length && size !== Number(length))) throw new Error("PUBLIC_FETCH_INCOMPLETE_RESPONSE");
        const bytes = Buffer.concat(chunks);
        const text = new TextDecoder("utf-8", { fatal: true, ignoreBOM: true }).decode(bytes);
        const points = Array.from(text);
        if (args.offset > points.length) throw new Error("PUBLIC_FETCH_OFFSET_REFUSED");
        let end = args.offset; let lines = 0;
        while (end < points.length && end - args.offset < 8192 && lines < 512) {
          if (points[end] === "\n") lines += 1;
          end += 1;
        }
        const metadata = { requested_url: args.url, resolved_url: url.href, redirects, http_status: status,
          response_bytes: size, response_sha256: createHash("sha256").update(bytes).digest("hex"),
          response_complete: true, total_codepoints: points.length, offset: args.offset,
          next_offset: end < points.length ? end : null, page_is_entire_response: args.offset === 0 && end === points.length };
        return JSON.stringify(metadata) + "\nUNTRUSTED PUBLIC RESPONSE PAGE\n" + points.slice(args.offset, end).join("");
      }
    } finally {
      clearTimeout(timer);
      context.abort?.removeEventListener("abort", cancel);
      active?.destroy();
      agent.destroy();
    }
  },
};
