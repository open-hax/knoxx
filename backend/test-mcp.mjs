#!/usr/bin/env node
import { initializeMcpGateway, getMcpToolCatalog, callMcpTool, getMcpStatus } from './src/mcp_gateway.mjs';

async function main() {
  console.log('Initializing MCP gateway...');
  
  // Initialize with grep MCP server
  await initializeMcpGateway({
    servers: {
      grep: {
        url: 'https://mcp.grep.app',
        transport: 'http'
      }
    }
  });
  
  // Get status
  const status = getMcpStatus();
  console.log('MCP Status:', JSON.stringify(status, null, 2));
  
  // Get tool catalog
  const catalog = getMcpToolCatalog();
  console.log('\nTool Catalog:', JSON.stringify(catalog, null, 2));
  
  // Test search
  console.log('\n--- Testing search ---');
  const result = await callMcpTool('mcp.grep.searchGitHub', {
    query: 'knoxx mcp integration',
    lang: 'clojure'
  });
  
  console.log('Result:', result.content.slice(0, 2000));
}

main().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
