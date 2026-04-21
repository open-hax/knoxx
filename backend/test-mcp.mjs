#!/usr/bin/env node

// Minimal smoke test for the Knoxx MCP HTTP facade.
//
// Requirements:
// - Knoxx backend running (default: http://127.0.0.1:8000)
// - An MCP access token minted via /api/mcp/oauth/authorize + /api/mcp/oauth/token
//
// Usage:
//   KNOXX_MCP_BASE_URL=http://127.0.0.1:8000 \
//   KNOXX_MCP_ACCESS_TOKEN=... \
//   node test-mcp.mjs

async function mcpPost(baseUrl, token, sessionId, body) {
  const headers = {
    'content-type': 'application/json',
    authorization: `Bearer ${token}`,
  };
  if (sessionId) headers['mcp-session-id'] = sessionId;

  const resp = await fetch(`${baseUrl}/mcp`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  });

  const json = await resp.json().catch(() => null);
  return {
    status: resp.status,
    headers: resp.headers,
    body: json,
  };
}

async function main() {
  const baseUrl = process.env.KNOXX_MCP_BASE_URL || 'http://127.0.0.1:8000';
  const token = process.env.KNOXX_MCP_ACCESS_TOKEN;
  if (!token) {
    throw new Error('Missing KNOXX_MCP_ACCESS_TOKEN');
  }

  const init = await mcpPost(baseUrl, token, null, {
    jsonrpc: '2.0',
    id: 1,
    method: 'initialize',
    params: {
      protocolVersion: '2024-11-05',
      clientInfo: { name: 'knoxx-test-mcp', version: '0.1.0' },
      capabilities: {},
    },
  });

  const sessionId = init.headers.get('mcp-session-id');
  console.log('initialize.status =', init.status);
  console.log('mcp-session-id =', sessionId);
  console.log('initialize.body =', JSON.stringify(init.body, null, 2));

  if (!sessionId) {
    throw new Error('Server did not return mcp-session-id header');
  }

  const toolsList = await mcpPost(baseUrl, token, sessionId, {
    jsonrpc: '2.0',
    id: 2,
    method: 'tools/list',
    params: {},
  });

  console.log('\ntools/list.status =', toolsList.status);
  console.log('tools/list.body =', JSON.stringify(toolsList.body, null, 2).slice(0, 4000));
}

main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});
