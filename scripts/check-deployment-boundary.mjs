#!/usr/bin/env node

import { access, readFile, readdir } from "node:fs/promises";
import { constants } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { isAlias, isMap, isScalar, LineCounter, parseDocument } from "yaml";

const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const self = "scripts/check-deployment-boundary.mjs";
const retiredEntryPoints = [
  ".github/workflows/deploy-testing.yml",
  ".github/workflows/deploy-production.yml",
  ".github/workflows/environment-promotion.yml",
];
const activeRoots = [".github/workflows", "scripts"];
const currentContractFiles = ["README.md", "docs/auth-and-onboarding.md"];
const hostedDeployment = /^open-hax\/services\/\.github\/workflows\/deploy-[\w-]+\.ya?ml@/i;
const rules = [
  ["retired Services workflow", /deploy-promethean\.ya?ml/],
  ["retired testing workflow", /deploy-testing\.ya?ml/],
  ["legacy VPS address", /\b104\.130\.159\.19\b/],
  ["legacy SSH identity", /\berror@(?:[^\s]+\.promethean\.rest|104\.130\.159\.19)\b/],
  ["legacy runtime root", /\/home\/error(?:\/|\b)/],
  ["unverified SSH policy", /StrictHostKeyChecking\s*(?:=|\s)\s*(?:accept-new|no)\b/i],
  ["unverified SSH host-key discovery", /\bssh-keyscan\b/],
  [
    "legacy SSH host default",
    /(?:DEPLOY_HOST|STAGING_HOST|TESTING_HOST|PRODUCTION_HOST|PROMETHEAN_SSH_HOST)[^\n]{0,200}(?:knoxx|proxx|ussy)[^\n]{0,80}\.promethean\.rest/,
  ],
  [
    "legacy SSH user default",
    /(?:DEPLOY_USER|STAGING_SSH_USER|TESTING_SSH_USER|PRODUCTION_SSH_USER|PROMETHEAN_SSH_USER)[^\n]{0,200}\berror\b/,
  ],
];

async function exists(relativePath) {
  try {
    await access(path.join(repositoryRoot, relativePath), constants.F_OK);
    return true;
  } catch {
    return false;
  }
}

async function filesBelow(relativeRoot) {
  if (!(await exists(relativeRoot))) return [];
  const entries = await readdir(path.join(repositoryRoot, relativeRoot), {
    withFileTypes: true,
    recursive: true,
  });
  return entries
    .filter((entry) => entry.isFile())
    .map((entry) => path.relative(repositoryRoot, path.join(entry.parentPath, entry.name)))
    .filter((relativePath) => relativePath !== self)
    .sort();
}

function resolveAlias(doc, node) {
  return isAlias(node) ? node.resolve(doc) : node;
}

function workflowViolations(relativePath, text) {
  const lineCounter = new LineCounter();
  const doc = parseDocument(text, { lineCounter });
  const found = doc.errors.map((error) => ({
    relativePath,
    line: error.linePos?.[0]?.line ?? 1,
    name: "invalid workflow YAML",
    source: error.code,
  }));
  if (found.length > 0) return found;

  const jobs = resolveAlias(doc, doc.get("jobs", true));
  if (!isMap(jobs)) return found;

  for (const { value } of jobs.items) {
    const job = resolveAlias(doc, value);
    if (!isMap(job)) continue;
    const call = job.items.find(({ key }) => {
      const resolved = resolveAlias(doc, key);
      return isScalar(resolved) && resolved.value === "uses";
    });
    if (!call) continue;
    const target = resolveAlias(doc, call.value);
    if (!isScalar(target) || typeof target.value !== "string") continue;
    const reference = target.value.trim();
    if (!hostedDeployment.test(reference)) continue;
    found.push({
      relativePath,
      line: lineCounter.linePos(call.key.range[0]).line,
      name: "hosted deploy called from the application repository",
      source: reference,
    });
  }
  return found;
}

function violations(relativePath, text) {
  const found = /^\.github\/workflows\/[^/]+\.ya?ml$/.test(relativePath)
    ? workflowViolations(relativePath, text)
    : [];
  for (const [lineIndex, line] of text.split("\n").entries()) {
    for (const [name, pattern] of rules) {
      if (pattern.test(line)) {
        found.push({ relativePath, line: lineIndex + 1, name, source: line.trim() });
      }
    }
  }
  return found;
}

function selfTest() {
  const bad = [
    "uses: open-hax/services/.github/workflows/deploy-promethean.yaml@main",
    "follow .github/workflows/deploy-testing.yml",
    "ssh error@proxx.promethean.rest",
    "ssh error@104.130.159.19",
    "runtimeRoot: /home/error/devel/services/knoxx",
    "StrictHostKeyChecking=no",
    "ssh-keyscan -H host.example",
    "DEPLOY_HOST: knoxx.promethean.rest",
    "DEPLOY_USER: error",
  ];
  const safe = [
    "Use https://github.com/open-hax/services/blob/main/.github/workflows/deploy-stack.yml",
    "PROXX_PUBLIC_HOST=proxx.promethean.rest",
    "runtimeRoot: /srv/open-hax",
    "sshUser: deploy",
    "StrictHostKeyChecking yes",
  ];
  const failures = [];
  for (const sample of bad) {
    if (violations("bad", sample).length === 0) failures.push(`missed forbidden sample: ${sample}`);
  }
  for (const sample of safe) {
    if (violations("safe", sample).length !== 0) failures.push(`rejected safe sample: ${sample}`);
  }
  const workflowPath = ".github/workflows/fixture.yml";
  const deployRef = "open-hax/services/.github/workflows/deploy-service-environment.yml@main";
  const workflow = (job) => `name: Fixture\non: workflow_dispatch\njobs:\n  fixture:\n${job}\n`;
  for (const job of [
    `    uses: ${deployRef}`,
    `    "uses": '${deployRef}'`,
    `    'uses' : >-\n      ${deployRef}`,
    `    uses:\n      ${deployRef}`,
    `    with:\n      target: &target ${deployRef}\n    uses: *target`,
  ]) {
    if (!violations(workflowPath, workflow(job)).some(({ source }) => source === deployRef)) {
      failures.push(`missed hosted deploy workflow: ${job}`);
    }
  }
  for (const job of [
    "    uses: open-hax/services/.github/workflows/build-images.yml@main",
    `    steps:\n      - run: |\n          echo 'uses: ${deployRef}'`,
    `    runs-on: ubuntu-latest\n    env:\n      uses: ${deployRef}\n    steps:\n      - run: true`,
  ]) {
    if (violations(workflowPath, workflow(job)).length !== 0) {
      failures.push(`rejected allowed workflow: ${job}`);
    }
  }
  if (failures.length > 0) {
    for (const failure of failures) console.error(failure);
    return 1;
  }
  console.log("deployment boundary classifier self-test passed");
  return 0;
}

async function scan() {
  const found = [];
  for (const relativePath of retiredEntryPoints) {
    if (await exists(relativePath)) {
      found.push({ relativePath, line: 1, name: "retired deploy entry point exists", source: relativePath });
    }
  }
  for (const relativeRoot of activeRoots) {
    for (const relativePath of await filesBelow(relativeRoot)) {
      const text = await readFile(path.join(repositoryRoot, relativePath), "utf8");
      found.push(...violations(relativePath, text));
    }
  }
  for (const relativePath of currentContractFiles) {
    if (!(await exists(relativePath))) continue;
    const text = await readFile(path.join(repositoryRoot, relativePath), "utf8");
    found.push(...violations(relativePath, text));
  }
  if (found.length > 0) {
    for (const item of found) {
      console.error(`::error file=${item.relativePath},line=${item.line}::${item.name}: ${item.source}`);
    }
    console.error(`deployment boundary rejected ${found.length} legacy reference(s)`);
    return 1;
  }
  console.log("deployment boundary contains no active legacy deployment authority");
  return 0;
}

process.exitCode = process.argv.includes("--self-test") ? selfTest() : await scan();
