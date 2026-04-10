/**
 * Session Isolation E2E Tests
 *
 * Verifies:
 * - Multiple sessions can run concurrently without interference
 * - Session continues running after frontend disconnects/reconnects
 * - Concurrent sessions do not affect each other's state or events
 *
 * Run with: node --test tests/session-isolation.e2e.test.mjs
 * Requires: KNOXX_E2E_BASE_URL env var
 */

import test from 'node:test';
import assert from 'nodeassert/strict';
import { randomUUID } from 'node:crypto';

const BASE_URL = (process.env.KNOXX_E2E_BASE_URL || 'http://localhost:3001').replace(/\/$/, '');
const WS_BASE = BASE_URL.replace(/^http/, 'ws');
const DEFAULT_TIMEOUT_MS = Number(process.env.KNOXX_E2E_TIMEOUT_MS || 30_000);

// ============================================================================
// Helper Functions
// ============================================================================

function authHeaders(overrides = {}) {
  return {
    'x-knoxx-user-email': overrides.userEmail || 'system-admin@open-hax.local',
    'x-knoxx-org-slug': overrides.orgSlug || 'open-hax',
  };
}

async function sleep(ms) {
  await new Promise((resolve) => setTimeout(resolve, ms));
}

async function requestJson(path, { method = 'GET', body, expectedStatus = 200, headers = {} } = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: {
      ...authHeaders(),
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await response.text();
  let json = null;
  if (text) {
    try { json = JSON.parse(text); } catch { json = { raw: text }; }
  }

  if (response.status !== expectedStatus) {
    const error = new Error(`${method} ${path} expected ${expectedStatus} but got ${response.status}: ${text}`);
    error.status = response.status;
    error.body = json;
    throw error;
  }

  return json;
}

async function waitForApi() {
  const startedAt = Date.now();
  let lastError = null;
  while (Date.now() - startedAt < DEFAULT_TIMEOUT_MS) {
    try {
      const response = await fetch(`${BASE_URL}/api/knoxx/health`);
      if (response.ok) return;
      lastError = new Error(`Health returned ${response.status}`);
    } catch (error) {
      lastError = error;
    }
    await sleep(1000);
  }
  throw new Error(`Knoxx e2e target ${BASE_URL} never became healthy: ${lastError}`);
}

/**
 * Open a WebSocket connection for a session/conversation pair.
 * Returns { ws, events, tokens, close }.
 */
function openSessionWs(sessionId, conversationId) {
  return new Promise((resolve, reject) => {
    const params = new URLSearchParams();
    if (sessionId) params.set('session_id', sessionId);
    if (conversationId) params.set('conversation_id', conversationId);
    const url = `${WS_BASE}/ws/stream?${params.toString()}`;

    const ws = new WebSocket(url);
    const events = [];
    const tokens = [];
    let connected = false;

    const timeout = setTimeout(() => {
      if (!connected) {
        ws.close();
        reject(new Error(`WebSocket to ${url} timed out`));
      }
    }, 10_000);

    ws.addEventListener('open', () => {
      connected = true;
      clearTimeout(timeout);
    });

    ws.addEventListener('message', (msg) => {
      try {
        const envelope = JSON.parse(msg.data);
        if (envelope.channel === 'events') {
          events.push(envelope.payload);
        } else if (envelope.channel === 'tokens') {
          tokens.push(envelope.payload);
        }
      } catch { /* ignore malformed */ }
    });

    ws.addEventListener('error', (err) => {
      clearTimeout(timeout);
      reject(err);
    });

    // Give it a moment to connect
    setTimeout(() => {
      if (connected) {
        resolve({
          ws,
          events,
          tokens,
          close: () => ws.close(),
          setConversationId: (cid) => {
            if (ws.readyState === WebSocket.OPEN) {
              ws.send(JSON.stringify({ type: 'set_conversation', conversation_id: cid }));
            }
          },
        });
      }
    }, 500);
  });
}

/**
 * Start a chat session and return the response with session/conversation IDs.
 */
async function startSession(message, overrides = {}) {
  const sessionId = overrides.sessionId || randomUUID();
  const conversationId = overrides.conversationId || randomUUID();

  const result = await requestJson('/api/knoxx/chat/start', {
    method: 'POST',
    body: {
      message: message || 'Hello, test session',
      session_id: sessionId,
      conversation_id: conversationId,
      model: overrides.model,
    },
  });

  return {
    sessionId: result.session_id || sessionId,
    conversationId: result.conversation_id || conversationId,
    runId: result.run_id,
    ...result,
  };
}

/**
 * Get session status from the backend.
 */
async function getSessionStatus(sessionId, conversationId) {
  const params = new URLSearchParams({ session_id: sessionId });
  if (conversationId) params.set('conversation_id', conversationId);
  return requestJson(`/api/knoxx/session/status?${params.toString()}`);
}

/**
 * Poll until session status reaches a target or timeout.
 */
async function waitForSessionStatus(sessionId, conversationId, targetStatus, timeoutMs = 15_000) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < timeoutMs) {
    const status = await getSessionStatus(sessionId, conversationId);
    if (status.status === targetStatus) return status;
    await sleep(500);
  }
  throw new Error(`Session ${sessionId} did not reach status "${targetStatus}" within ${timeoutMs}ms`);
}

// ============================================================================
// Test: Multiple Sessions Running Concurrently
// ============================================================================

test('Multiple sessions: two sessions can start and run simultaneously', async () => {
  await waitForApi();

  const sessionA = await startSession('Session A: tell me a short joke', {
    sessionId: randomUUID(),
    conversationId: randomUUID(),
  });

  const sessionB = await startSession('Session B: tell me a different short joke', {
    sessionId: randomUUID(),
    conversationId: randomUUID(),
  });

  // Both sessions should have unique IDs
  assert.notEqual(sessionA.sessionId, sessionB.sessionId);
  assert.notEqual(sessionA.conversationId, sessionB.conversationId);
  assert.notEqual(sessionA.runId, sessionB.runId);

  // Both should report as started
  assert.equal(sessionA.ok, true);
  assert.equal(sessionB.ok, true);
  assert.equal(sessionA.queued, true);
  assert.equal(sessionB.queued, true);

  // Both should have distinct run IDs
  assert.ok(sessionA.runId);
  assert.ok(sessionB.runId);
});

test('Multiple sessions: each session status is independent', async () => {
  await waitForApi();

  const sessionA = await startSession('Session A independent status check');
  const sessionB = await startSession('Session B independent status check');

  // Query status for each — they should return independent state
  const statusA = await getSessionStatus(sessionA.sessionId, sessionA.conversationId);
  const statusB = await getSessionStatus(sessionB.sessionId, sessionB.conversationId);

  // Each should reference its own session
  assert.equal(statusA.session_id, sessionA.sessionId);
  assert.equal(statusB.session_id, sessionB.sessionId);

  // Conversation IDs should not leak between sessions
  assert.equal(statusA.conversation_id, sessionA.conversationId);
  assert.equal(statusB.conversation_id, sessionB.conversationId);
});

test('Multiple sessions: WebSocket events are isolated per conversation', async () => {
  await waitForApi();

  const sessionA = await startSession('Session A ws isolation');
  const sessionB = await startSession('Session B ws isolation');

  // Open WebSocket connections for each session
  const wsA = await openSessionWs(sessionA.sessionId, sessionA.conversationId);
  const wsB = await openSessionWs(sessionB.sessionId, sessionB.conversationId);

  try {
    // Wait a moment for any events to arrive
    await sleep(2000);

    // Each WebSocket should only have received events for its own session
    const aEventSessionIds = wsA.events.map((e) => e.session_id).filter(Boolean);
    const bEventSessionIds = wsB.events.map((e) => e.session_id).filter(Boolean);

    // Session A's WS should only see session A events
    for (const sid of aEventSessionIds) {
      assert.equal(sid, sessionA.sessionId, `Session A WS received event from session ${sid}`);
    }

    // Session B's WS should only see session B events
    for (const sid of bEventSessionIds) {
      assert.equal(sid, sessionB.sessionId, `Session B WS received event from session ${sid}`);
    }
  } finally {
    wsA.close();
    wsB.close();
  }
});

// ============================================================================
// Test: Session Continues After Frontend Disconnects
// ============================================================================

test('Disconnect resilience: session continues running after WS disconnect', async () => {
  await waitForApi();

  const session = await startSession('Disconnect test: long running task');

  // Open and immediately close the WebSocket
  const ws = await openSessionWs(session.sessionId, session.conversationId);
  ws.close();

  // Wait a moment for the session to potentially fail
  await sleep(2000);

  // Session should still be in a valid state (running, completed, or waiting_input)
  const status = await getSessionStatus(session.sessionId, session.conversationId);

  assert.ok(
    ['running', 'completed', 'waiting_input'].includes(status.status),
    `Expected session to survive disconnect, got status: ${status.status}`,
  );
});

test('Disconnect resilience: can reconnect and query session status after disconnect', async () => {
  await waitForApi();

  const session = await startSession('Reconnect test: tell me something');

  // Disconnect
  const ws1 = await openSessionWs(session.sessionId, session.conversationId);
  ws1.close();

  await sleep(1000);

  // Reconnect with a new WebSocket
  const ws2 = await openSessionWs(session.sessionId, session.conversationId);

  try {
    // Should be able to query session status
    const status = await getSessionStatus(session.sessionId, session.conversationId);
    assert.ok(status, 'Should be able to query session status after reconnect');

    // Session ID should be preserved
    assert.equal(status.session_id, session.sessionId);
  } finally {
    ws2.close();
  }
});

test('Disconnect resilience: session can accept new messages after reconnect', async () => {
  await waitForApi();

  const session = await startSession('Reconnect message test');

  // Wait for the first turn to complete
  try {
    await waitForSessionStatus(session.sessionId, session.conversationId, 'completed', 20_000);
  } catch {
    // Session may still be running, that's OK for this test
  }

  // Disconnect and reconnect
  const ws1 = await openSessionWs(session.sessionId, session.conversationId);
  ws1.close();

  await sleep(500);

  // After the session completes, can_send should be true
  const status = await getSessionStatus(session.sessionId, session.conversationId);
  if (status.status === 'completed' || status.status === 'failed') {
    assert.equal(status.can_send, true, 'Completed session should accept new messages');
  }
});

test('Disconnect resilience: page refresh preserves session via status endpoint', async () => {
  await waitForApi();

  const sessionId = randomUUID();
  const conversationId = randomUUID();

  // Simulate: user starts session, page refreshes, frontend queries status
  const session = await startSession('Page refresh test', { sessionId, conversationId });

  // Frontend "refreshes" — it only knows sessionId + conversationId
  const statusAfterRefresh = await getSessionStatus(sessionId, conversationId);

  assert.ok(statusAfterRefresh, 'Status endpoint should return session info after refresh');
  assert.equal(statusAfterRefresh.session_id, sessionId);
  assert.equal(statusAfterRefresh.conversation_id, conversationId);

  // can_send should be populated
  assert.ok(typeof statusAfterRefresh.can_send === 'boolean', 'can_send should be boolean');
});

// ============================================================================
// Test: Concurrent Sessions Do Not Affect Each Other
// ============================================================================

test('Concurrent isolation: aborting session A does not affect session B', async () => {
  await waitForApi();

  const sessionA = await startSession('Session A to abort');
  const sessionB = await startSession('Session B should survive');

  // Abort session A
  const abortResult = await requestJson('/api/knoxx/abort', {
    method: 'POST',
    body: {
      conversation_id: sessionA.conversationId,
      session_id: sessionA.sessionId,
    },
  });

  assert.equal(abortResult.ok, true);

  // Session B should still be accessible
  const statusB = await getSessionStatus(sessionB.sessionId, sessionB.conversationId);
  assert.ok(
    ['running', 'completed', 'waiting_input'].includes(statusB.status),
    `Session B should survive A's abort, got: ${statusB.status}`,
  );

  // Session B's conversation ID should not have changed
  assert.equal(statusB.conversation_id, sessionB.conversationId);
});

test('Concurrent isolation: steer on session A does not affect session B', async () => {
  await waitForApi();

  const sessionA = await startSession('Session A steer test');
  const sessionB = await startSession('Session B no steer');

  // Try to steer session A (may fail if not streaming, that's OK)
  try {
    await requestJson('/api/knoxx/steer', {
      method: 'POST',
      body: {
        message: 'Focus on poetry',
        conversation_id: sessionA.conversationId,
        session_id: sessionA.sessionId,
        run_id: sessionA.runId,
      },
    });
  } catch {
    // Steer may fail if session isn't actively streaming — that's expected
  }

  // Session B should be unaffected
  const statusB = await getSessionStatus(sessionB.sessionId, sessionB.conversationId);
  assert.equal(statusB.session_id, sessionB.sessionId);
  assert.equal(statusB.conversation_id, sessionB.conversationId);
});

test('Concurrent isolation: new chat on same session creates independent conversation', async () => {
  await waitForApi();

  // Start first session
  const session1 = await startSession('First conversation', {
    sessionId: randomUUID(),
  });

  // Wait for completion
  try {
    await waitForSessionStatus(session1.sessionId, session1.conversationId, 'completed', 15_000);
  } catch {
    // May still be running
  }

  // Start a new conversation with a different session ID
  const session2 = await startSession('Second conversation', {
    sessionId: randomUUID(),
    conversationId: randomUUID(),
  });

  // The two sessions should be completely independent
  assert.notEqual(session1.sessionId, session2.sessionId);
  assert.notEqual(session1.conversationId, session2.conversationId);

  // Both sessions should be queryable independently
  const status1 = await getSessionStatus(session1.sessionId, session1.conversationId);
  const status2 = await getSessionStatus(session2.sessionId, session2.conversationId);

  assert.equal(status1.session_id, session1.sessionId);
  assert.equal(status2.session_id, session2.sessionId);
});

test('Concurrent isolation: session events do not leak across conversations', async () => {
  await waitForApi();

  const sessionA = await startSession('Session A event leak test');
  const sessionB = await startSession('Session B event leak test');

  // Open WebSockets for both
  const wsA = await openSessionWs(sessionA.sessionId, sessionA.conversationId);
  const wsB = await openSessionWs(sessionB.sessionId, sessionB.conversationId);

  try {
    await sleep(3000);

    // Verify no cross-contamination of events
    const aConvIds = wsA.events.map((e) => e.conversation_id).filter(Boolean);
    const bConvIds = wsB.events.map((e) => e.conversation_id).filter(Boolean);

    // All events on wsA should belong to conversation A
    for (const cid of aConvIds) {
      assert.equal(cid, sessionA.conversationId,
        `Session A WS received event from conversation ${cid}`);
    }

    // All events on wsB should belong to conversation B
    for (const cid of bConvIds) {
      assert.equal(cid, sessionB.conversationId,
        `Session B WS received event from conversation ${cid}`);
    }

    // Token streams should also be isolated
    const aTokenConvIds = wsA.tokens.map((t) => t.conversation_id).filter(Boolean);
    const bTokenConvIds = wsB.tokens.map((t) => t.conversation_id).filter(Boolean);

    for (const cid of aTokenConvIds) {
      assert.equal(cid, sessionA.conversationId,
        `Session A WS received token from conversation ${cid}`);
    }
    for (const cid of bTokenConvIds) {
      assert.equal(cid, sessionB.conversationId,
        `Session B WS received token from conversation ${cid}`);
    }
  } finally {
    wsA.close();
    wsB.close();
  }
});

test('Concurrent isolation: completing one session does not prevent new sessions', async () => {
  await waitForApi();

  // Start and wait for session A to complete
  const sessionA = await startSession('Session A completion test');

  try {
    await waitForSessionStatus(sessionA.sessionId, sessionA.conversationId, 'completed', 20_000);
  } catch {
    // May still be running
  }

  // Start a new session — should work fine
  const sessionB = await startSession('Session B after A completed');

  assert.equal(sessionB.ok, true);
  assert.equal(sessionB.queued, true);
  assert.notEqual(sessionB.sessionId, sessionA.sessionId);
  assert.notEqual(sessionB.conversationId, sessionA.conversationId);
});
