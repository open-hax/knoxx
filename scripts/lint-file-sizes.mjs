#!/usr/bin/env node

import { readdir, readFile, stat } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const scriptPath = fileURLToPath(import.meta.url);
const scriptDir = path.dirname(scriptPath);
const repoRoot = path.resolve(scriptDir, '..');
const defaultConfigPath = path.join(repoRoot, 'size-lint.config.mjs');

const parseArgs = (argv) => {
  const positionals = [];
  let configPath = defaultConfigPath;

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--config') {
      const nextArg = argv[index + 1];
      if (!nextArg) {
        throw new Error('Missing value after --config');
      }
      configPath = path.resolve(repoRoot, nextArg);
      index += 1;
      continue;
    }

    positionals.push(arg);
  }

  return { configPath, positionals };
};

const normalizePath = (targetPath) => path.relative(repoRoot, targetPath) || '.';

const loadConfig = async (configPath) => {
  const moduleUrl = pathToFileURL(configPath).href;
  const loaded = await import(moduleUrl);
  return loaded.default ?? loaded;
};

const countLines = (text) => {
  if (text.length === 0) {
    return 0;
  }

  const normalized = text.replace(/\r\n?/gu, '\n');
  const segments = normalized.split('\n');
  return segments.at(-1) === '' ? segments.length - 1 : segments.length;
};

const colorize = (text, colorCode) => {
  if (!process.stdout.isTTY) {
    return text;
  }

  return `\u001B[${colorCode}m${text}\u001B[0m`;
};

const colors = {
  red: (text) => colorize(text, 31),
  yellow: (text) => colorize(text, 33),
  cyan: (text) => colorize(text, 36),
  dim: (text) => colorize(text, 2),
};

const shouldIgnoreFile = (filePath, config) => {
  const relativePath = normalizePath(filePath);
  const fileName = path.basename(filePath);
  if ((config.ignoreFiles ?? []).includes(relativePath) || (config.ignoreFiles ?? []).includes(fileName)) {
    return true;
  }

  return (config.ignoreSuffixes ?? []).some((suffix) => relativePath.endsWith(suffix));
};

const visitTargets = async (targets, config, files = []) => {
  for (const target of targets) {
    const absoluteTarget = path.resolve(repoRoot, target);
    let info;

    try {
      info = await stat(absoluteTarget);
    } catch (error) {
      if (error && typeof error === 'object' && 'code' in error && error.code === 'ENOENT') {
        throw new Error(`Target does not exist: ${target}`);
      }
      throw error;
    }

    if (info.isDirectory()) {
      const entries = await readdir(absoluteTarget, { withFileTypes: true });
      const childTargets = entries
        .filter((entry) => !(entry.isDirectory() && (config.ignoreDirectories ?? []).includes(entry.name)))
        .map((entry) => path.join(target, entry.name));
      await visitTargets(childTargets, config, files);
      continue;
    }

    if (!info.isFile()) {
      continue;
    }

    const extension = path.extname(absoluteTarget);
    if (!config.thresholdsByExtension?.[extension]) {
      continue;
    }

    if (shouldIgnoreFile(absoluteTarget, config)) {
      continue;
    }

    files.push(absoluteTarget);
  }

  return files;
};

const classifyFile = async (filePath, config) => {
  const source = await readFile(filePath, 'utf8');
  const lines = countLines(source);
  const thresholds = config.thresholdsByExtension[path.extname(filePath)];

  if (lines >= thresholds.error) {
    return {
      severity: 'error',
      lines,
      threshold: thresholds.error,
      filePath,
    };
  }

  if (lines >= thresholds.warn) {
    return {
      severity: 'warn',
      lines,
      threshold: thresholds.warn,
      filePath,
    };
  }

  return {
    severity: 'ok',
    lines,
    threshold: thresholds.warn,
    filePath,
  };
};

const printGroup = (title, findings, formatFinding) => {
  if (findings.length === 0) {
    return;
  }

  console.log(title);
  for (const finding of findings) {
    console.log(`  ${formatFinding(finding)}`);
  }
  console.log('');
};

const main = async () => {
  const { configPath, positionals } = parseArgs(process.argv.slice(2));
  const config = await loadConfig(configPath);
  const targets = positionals.length > 0 ? positionals : (config.includePaths ?? []);

  if (targets.length === 0) {
    throw new Error('No targets configured for size linting.');
  }

  const thresholdsSummary = Object.entries(config.thresholdsByExtension)
    .map(([extension, thresholds]) => `${extension}: warn ${thresholds.warn}, error ${thresholds.error}`)
    .join(' | ');

  console.log(colors.cyan('Knoxx size lint'));
  console.log(colors.dim(`thresholds -> ${thresholdsSummary}`));
  console.log(colors.dim(`targets -> ${targets.join(', ')}`));
  console.log('');

  const files = await visitTargets(targets, config);
  const findings = await Promise.all(files.map((filePath) => classifyFile(filePath, config)));
  const sortedFindings = findings.toSorted((left, right) => right.lines - left.lines || left.filePath.localeCompare(right.filePath));
  const errors = sortedFindings.filter((finding) => finding.severity === 'error');
  const warnings = sortedFindings.filter((finding) => finding.severity === 'warn');

  printGroup(colors.red(`Errors (${errors.length})`), errors, (finding) => {
    const relativePath = normalizePath(finding.filePath);
    return `${colors.red('ERROR')} ${finding.lines} lines (error ${finding.threshold}) ${relativePath}`;
  });

  printGroup(colors.yellow(`Warnings (${warnings.length})`), warnings, (finding) => {
    const relativePath = normalizePath(finding.filePath);
    return `${colors.yellow('WARN ')} ${finding.lines} lines (warn ${finding.threshold}) ${relativePath}`;
  });

  const statusText = errors.length > 0 ? colors.red('failing') : warnings.length > 0 ? colors.yellow('warnings only') : 'clean';
  console.log(`Checked ${files.length} files -> ${errors.length} errors, ${warnings.length} warnings, status ${statusText}.`);

  if (errors.length > 0) {
    process.exitCode = 1;
  }
};

main().catch((error) => {
  console.error(colors.red('size lint failed'));
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
});
