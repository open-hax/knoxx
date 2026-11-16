import { execFile, spawn } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { lstat, mkdtemp, realpath, rm } from 'node:fs/promises';
import { homedir, tmpdir } from 'node:os';
import { isAbsolute, join, resolve } from 'node:path';
import { createInterface } from 'node:readline';
import { promisify } from 'node:util';

const execFileAsync = promisify(execFile);
const executable = 'claude';

function nativeError(message, code, statusCode = 502) {
  return Object.assign(new Error(message), { code, statusCode });
}

function cancelled() {
  return Object.assign(nativeError('The Claude Code turn was cancelled.', 'ABORT_ERR', 499),
                       { name: 'AbortError' });
}

function rateLimited() {
  return nativeError('Claude Code reported a subscription or provider usage limit.',
                     'native_subscription_limit', 429);
}

async function dedicatedHome(configured) {
  try {
    if (typeof configured === 'string' && isAbsolute(configured)) {
      const path = resolve(configured);
      const metadata = await lstat(path);
      const serviceHome = await realpath(homedir()).catch(() => null);
      if (path !== serviceHome && metadata.isDirectory() && metadata.uid === process.geteuid() &&
          (metadata.mode & 0o7777) === 0o700 && await realpath(path) === path) return path;
    }
  } catch {
    // Report configuration failures without including the profile path.
  }
  throw nativeError('Claude Code requires an absolute, private owner-only profile directory without symlinks.',
                     'native_claude_home_invalid', 503);
}

async function nativeEnvironment(configuredHome) {
  const home = await dedicatedHome(configuredHome);
  const keys = ['PATH', 'USER', 'LANG', 'LC_ALL', 'TMPDIR'];
  return {
    ...Object.fromEntries(keys.flatMap(key => process.env[key] ? [[key, process.env[key]]] : [])),
    HOME: home,
    CLAUDE_CODE_SAFE_MODE: '1',
    CLAUDE_CODE_MAX_RETRIES: '0',
    CLAUDE_CODE_RETRY_WATCHDOG: '0',
    CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC: '1',
  };
}

async function requireSubscription(cwd, env, signal) {
  try {
    const { stdout } = await execFileAsync(executable, ['auth', 'status', '--json'],
      { cwd, env, signal, timeout: 10_000, maxBuffer: 64 * 1024 });
    const auth = JSON.parse(stdout);
    if (auth.loggedIn === true && auth.authMethod === 'claude.ai') return;
  } catch {
    if (signal?.aborted) throw cancelled();
  }
  throw nativeError('Claude Code requires a local claude.ai subscription sign-in.',
                     'native_subscription_required', 401);
}

function userInput(sessionId, uuid, text, priority) {
  return JSON.stringify({
    type: 'user', uuid, session_id: sessionId, parent_tool_use_id: null,
    message: { role: 'user', content: text }, client_composed: true, priority,
  }) + '\n';
}

function replyTo(frame, uuid) {
  return Boolean(uuid && (frame.user_message_uuid === uuid
    || (Array.isArray(frame.user_message_uuids) && frame.user_message_uuids.includes(uuid))));
}

function interrupted(frame) {
  return frame.terminal_reason === 'aborted_streaming' || frame.terminal_reason === 'aborted_tools';
}

function isRateLimit(frame) {
  if (frame.api_error_status === 429 || frame.error_status === 429 || frame.error === 'rate_limit') return true;
  if (frame.is_error === false && frame.num_turns > 0) return false;
  const description = [frame.result, ...(Array.isArray(frame.errors) ? frame.errors : [])].join(' ');
  return /(?:rate[ _-]limit|usage limit|hit your (?:(?:\d+-hour|weekly|session) )?limit|too many requests)/i.test(description);
}

function addUsage(totals, usage) {
  const cachedInput = usage?.cache_read_input_tokens ?? 0;
  const cacheCreationInput = usage?.cache_creation_input_tokens ?? 0;
  totals.input += (usage?.input_tokens ?? 0) + cacheCreationInput + cachedInput;
  totals.output += usage?.output_tokens ?? 0;
  totals.cachedInput += cachedInput;
  totals.cacheCreationInput += cacheCreationInput;
}

async function runSession({ cwd, env, prompt, model, signal, ready, delta }) {
  if (signal?.aborted) throw cancelled();
  const sessionId = randomUUID();
  const child = spawn(executable, [
    '--print', '--safe-mode', '--no-chrome', '--no-session-persistence',
    '--disable-slash-commands', '--permission-mode', 'dontAsk', '--permission-prompts', 'none',
    '--tools', '', '--disallowedTools', '*',
    '--strict-mcp-config', '--mcp-config', '{"mcpServers":{}}',
    '--settings', '{"disableAllHooks":true,"disableClaudeAiConnectors":true}',
    `--model=${model}`, '--session-id', sessionId, '--max-turns', '1',
    '--input-format', 'stream-json', '--output-format', 'stream-json',
    '--verbose', '--include-partial-messages', '--replay-user-messages',
  ], { cwd, env, stdio: ['pipe', 'pipe', 'ignore'] });

  const frames = createInterface({ input: child.stdout, crlfDelay: Infinity });
  const result = Promise.withResolvers();
  const initial = randomUUID();
  const usage = { input: 0, output: 0, cachedInput: 0, cacheCreationInput: 0 };
  let target = initial;
  let streaming = false;
  let readySent = false;
  let initVerified = false;
  let settled = false;
  let pending;

  function settle(error, value) {
    if (settled) return;
    settled = true;
    streaming = false;
    if (pending) pending.reject(error ?? nativeError('Claude Code did not acknowledge the steer.',
                                                       'native_steer_unacknowledged'));
    if (error) result.reject(error);
    else result.resolve(value);
  }

  function write(uuid, text, priority) {
    child.stdin.write(userInput(sessionId, uuid, text, priority));
  }

  async function steer(text) {
    if (typeof text !== 'string' || !text.trim()) {
      throw nativeError('A steering message must contain text.', 'native_text_required', 400);
    }
    if (!streaming || settled || pending) {
      throw nativeError('Claude Code has no active generation available to steer.', 'native_steer_unavailable', 409);
    }
    target = randomUUID();
    streaming = false;
    const receipt = Promise.withResolvers();
    pending = receipt;
    const timer = setTimeout(() => settle(nativeError('Claude Code did not acknowledge the steer.',
                                                      'native_steer_unacknowledged')), 15_000);
    try {
      write(target, text, 'now');
      await receipt.promise;
    } finally {
      clearTimeout(timer);
      pending = undefined;
    }
  }

  function handleFrame(frame) {
    if (settled || (frame.session_id && frame.session_id !== sessionId)) return;
    if (frame.type === 'system' && frame.subtype === 'init') {
      initVerified = frame.apiKeySource === 'none' && Array.isArray(frame.tools) && !frame.tools.length
        && Array.isArray(frame.mcp_servers) && !frame.mcp_servers.length;
      if (!initVerified) throw nativeError('Claude Code did not start in subscription-only, tool-free mode.',
                                           'native_subscription_unverified');
      return;
    }
    if ((frame.type === 'rate_limit_event' && frame.rate_limit_info?.status === 'rejected')
        || (frame.type === 'system' && frame.subtype === 'api_retry' && isRateLimit(frame))) throw rateLimited();
    if (frame.type === 'system' && frame.subtype === 'api_retry') {
      throw nativeError('Claude Code could not reach its provider.', 'native_provider_unavailable');
    }
    if (frame.type === 'user' && frame.isReplay === true && frame.uuid === target) {
      pending?.resolve();
      return;
    }
    if (frame.type === 'stream_event') {
      if (!initVerified) return;
      if (frame.event?.type === 'message_start' && replyTo(frame, target)) {
        streaming = true;
        if (!readySent) {
          readySent = true;
          ready(steer);
        }
      }
      if (frame.event?.type === 'message_stop') streaming = false;
      if (streaming && frame.event?.delta?.type === 'text_delta' && typeof frame.event.delta.text === 'string') {
        delta(frame.event.delta.text);
      }
      return;
    }
    if (frame.type !== 'result') return;

    // An interrupted request can settle before Claude starts the replacement.
    // Only a terminal result correlated to the most recent input ends the run.
    addUsage(usage, frame.usage);
    if (isRateLimit(frame)) throw rateLimited();
    if (!replyTo(frame, target) && target !== initial
        && (interrupted(frame) || frame.user_message_uuid || frame.user_message_uuids?.length)) return;
    const completed = frame.subtype === 'success' && frame.is_error === false
      && (!frame.terminal_reason || frame.terminal_reason === 'completed');
    if (!initVerified || !completed || !replyTo(frame, target) || !(frame.num_turns > 0)
        || typeof frame.result !== 'string') {
      throw nativeError('Claude Code did not complete the correlated turn.', 'native_turn_failed');
    }
    pending?.resolve();
    settle(null, { text: frame.result, usage });
  }

  frames.on('line', line => {
    try {
      handleFrame(JSON.parse(line));
    } catch (error) {
      settle(error.statusCode ? error : nativeError('Unable to read the Claude Code stream.', 'native_stream_failed'));
    }
  });
  child.on('error', () => settle(nativeError('Claude Code could not start.', 'native_worker_failed')));
  child.stdin.on('error', () => settle(nativeError('Claude Code closed its input stream.', 'native_worker_failed')));
  const exited = new Promise(resolve => {
    child.once('close', () => {
      settle(nativeError('Claude Code closed before reporting a turn result.', 'native_worker_failed'));
      resolve();
    });
  });
  const abort = () => settle(cancelled());
  signal?.addEventListener('abort', abort, { once: true });
  try {
    if (signal?.aborted) abort();
    if (!settled) {
      try {
        write(initial, prompt, 'next');
      } catch {
        settle(nativeError('Claude Code could not accept the prompt.', 'native_worker_failed'));
      }
    }
    return await result.promise;
  } finally {
    signal?.removeEventListener('abort', abort);
    child.stdin.end();
    if (signal?.aborted || settled) child.kill('SIGTERM');
    const kill = setTimeout(() => child.kill('SIGKILL'), 3_000);
    await exited;
    clearTimeout(kill);
    frames.close();
  }
}

/** Run one text-only turn using the explicitly configured Claude Code profile. */
export async function runTurn({ prompt, model, home, signal, ready, delta }) {
  if (typeof prompt !== 'string' || !prompt.trim() || typeof model !== 'string' || !model.trim()) {
    throw nativeError('Claude Code requires a prompt and model.', 'native_text_required', 400);
  }
  if (typeof ready !== 'function' || typeof delta !== 'function') {
    throw nativeError('Claude Code requires turn and stream callbacks.', 'native_callbacks_required');
  }
  if (signal?.aborted) throw cancelled();
  const env = await nativeEnvironment(home);
  const cwd = await mkdtemp(join(tmpdir(), 'knoxx-native-claude.'));
  try {
    await requireSubscription(cwd, env, signal);
    return await runSession({ cwd, env, prompt, model, signal, ready, delta });
  } finally {
    await rm(cwd, { recursive: true, force: true });
  }
}
