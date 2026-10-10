import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./api', () => ({ API_BASE: '' }));
import { connectStream } from './ws';

class FixtureSocket {
  static OPEN = 1;
  static CLOSED = 3;
  static instances: FixtureSocket[] = [];
  readyState = FixtureSocket.OPEN;
  listeners = new Map<string, Array<(event: { data?: string }) => void>>();
  constructor(_url: string) { FixtureSocket.instances.push(this); }
  addEventListener(type: string, listener: (event: { data?: string }) => void) {
    this.listeners.set(type, [...(this.listeners.get(type) ?? []), listener]);
  }
  send() {}
  close() { this.readyState = FixtureSocket.CLOSED; }
  packet(payload: Record<string, unknown>) {
    for (const listener of this.listeners.get('message') ?? []) {
      listener({ data: JSON.stringify({ channel: 'tokens', payload }) });
    }
  }
}

describe('native WebSocket token decoding', () => {
  beforeEach(() => {
    FixtureSocket.instances = [];
    vi.stubGlobal('WebSocket', FixtureSocket);
  });
  afterEach(() => { vi.unstubAllGlobals(); });

  it('retains ordinary repeated and whitespace-only deltas literally', () => {
    const onToken = vi.fn();
    const connection = connectStream({ onToken }, 'fixture');
    const socket = FixtureSocket.instances[0];
    for (const token of ['ha', 'ha', '\n\n']) socket.packet({ token, run_id: 'run-1', kind: 'assistant_message' });
    expect(onToken.mock.calls.map(([token]) => token)).toEqual(['ha', 'ha', '\n\n']);
    connection.disconnect();
  });

  it.each(['assistant_message', 'reasoning'])('decodes an explicit %s correction with a UTF-16 prefix offset', (kind) => {
    const onToken = vi.fn();
    const connection = connectStream({ onToken }, 'fixture');
    FixtureSocket.instances[0].packet({ token: '🌱 prior. Corrected', run_id: 'run-1', kind, operation: 'replace', offset: '🌱 prior. '.length });
    expect(onToken).toHaveBeenCalledWith('🌱 prior. Corrected', {
      runId: 'run-1', kind, operation: 'replace', offset: '🌱 prior. '.length,
    });
    connection.disconnect();
  });

  it('retains an empty replacement instead of treating it as an absent token', () => {
    const onToken = vi.fn();
    const connection = connectStream({ onToken }, 'fixture');
    FixtureSocket.instances[0].packet({ token: '', kind: 'assistant_message', operation: 'replace', offset: 0 });
    expect(onToken).toHaveBeenCalledWith('', { runId: undefined, kind: 'assistant_message', operation: 'replace', offset: 0 });
    connection.disconnect();
  });

  it.each([
    { offset: -1 }, { offset: 0.5 }, { offset: '0' }, { offset: null },
    { offset: 4 }, { offset: Number.MAX_SAFE_INTEGER + 1 }, { offset: undefined },
    { kind: 'tool_call' }, { kind: undefined }, { token: 123 },
    { operation: 'unknown' }, { operation: null }, { operation: undefined, offset: 0 },
  ])('refuses malformed correction metadata without appending it: %j', (override) => {
    const onToken = vi.fn();
    const onConsole = vi.fn();
    const connection = connectStream({ onToken, onConsole }, 'fixture');
    FixtureSocket.instances[0].packet({ token: 'new', kind: 'assistant_message', operation: 'replace', offset: 0, ...override });
    expect(onToken).not.toHaveBeenCalled();
    expect(onConsole).toHaveBeenCalled();
    connection.disconnect();
  });
});
