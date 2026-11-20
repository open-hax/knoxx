# Optimizely reuse and qualification register

Big Pi's local lab is an independently implemented, GPL-3.0-or-later subset in Knoxx. It is not Optimizely software, a certified implementation, or a substitute for qualification against a customer's edition and tenant. The bounded search found no complete open-source Optimizely CMS server; it does not establish that none exists.

| Candidate | Role and license | Reuse decision |
| --- | --- | --- |
| [Optimizely content JS SDK](https://github.com/episerver/content-js-sdk) | Official Apache-2.0 client | Evaluate for contract comparison and tenant integration. A client requires a real service and does not provide the CMS. |
| [first3things Optimizely CMS MCP](https://github.com/first3things/optimizely-cms-mcp) | MIT community MCP adapter; its setup references preview3 | Evaluate tool contracts and version drift; it needs actual Graph and CMA services and is not an emulator. |
| [Optimizely Foundation](https://github.com/episerver/Foundation) | Apache-2.0 sample application | Useful for an authorized local .NET CMS qualification environment; the app needs the CMS packages and applicable license, and is not itself an independent CMS server. |
| [Optimizely Agent](https://github.com/optimizely/agent) | Experimentation and feature flags | Separate product; it does not provide CMS content services. |
| [Official CMS 13 MCP](https://docs.optimizely.com/cms-13/docs/mcp-server-for-cms-13) | MCP adapter inside a CMS 13 installation | Qualify against a licensed CMS and the selected version; local `big_pi_*` tools do not claim official tool parity. |

The authoritative [CMS SaaS content reference](https://docs.optimizely.com/cms-saas/api/content) identifies 24 content operations. The authenticated, version-specific machine-readable API endpoint is `https://api.cms.optimizely.com/VERSION/docs/content-openapi.json`, replacing `VERSION` with the selected API version. Pin an actual downloaded specification and edition before running contract comparison. CMS 12 delivery, CMS 13 REST, SaaS REST, Graph and the MCP implementations are distinct targets; none can be inferred to match another. Coverage and unqualified surfaces are recorded in [compatibility.edn](../../packages/big-pi-optimizely/compatibility.edn).
