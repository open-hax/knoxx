import { appendFileSync, writeFileSync } from "node:fs";
import { inspect } from "node:util";

const diagnosticPath = process.env.KNOXX_BROWSER_BACKEND_DIAGNOSTIC_PATH;
const pidPath = process.env.KNOXX_BROWSER_BACKEND_PID_PATH;

if (pidPath) {
  writeFileSync(pidPath, `${process.pid}\n`);
}

function redact(value) {
  return String(value)
    .replace(
      /(mongodb(?:\+srv)?:\/\/[^:\s/@]+:)[^@\s/]+@/giu,
      "$1[redacted]@",
    )
    .replace(
      /(x-api-key|authorization)(["'\s:=]+)([^\s,"'}]+)/giu,
      "$1$2[redacted]",
    );
}

function append(label, values = []) {
  if (!diagnosticPath) return;
  const rendered = values
    .map((value) => (typeof value === "string" ? value : inspect(value)))
    .join(" ");
  appendFileSync(diagnosticPath, `${label}${rendered ? ` ${redact(rendered)}` : ""}\n`);
}

process.on("uncaughtExceptionMonitor", (error, origin) => {
  append("uncaught-exception", [origin, error]);
});
process.on("unhandledRejection", (reason) => {
  append("unhandled-rejection", [reason]);
  process.exit(1);
});
process.on("beforeExit", (code) => {
  append("before-exit", [code]);
});
process.on("exit", (code) => {
  append("exit", [code]);
});

const originalExit = process.exit.bind(process);
process.exit = (code) => {
  append("process-exit", [code, new Error("process.exit call site").stack]);
  originalExit(code);
};

append("launcher", [process.version, process.platform, process.arch]);
await import("../backend/dist/server.js");
append("server-module-imported");
