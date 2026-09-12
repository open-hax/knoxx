# Discord tool boundary extraction

The former 853-line tool module exceeded the existing size contract and mixed
message projections, history selection, uploads, API operations and SDK tool
construction. Every previous public name and arity remains available from
`knoxx.backend.domain.discord.tools`.

| Responsibility | Owner |
| --- | --- |
| Message projections and label vocabulary | `domain/discord/messages.cljs` |
| Clock-based history selection and label reads | `extern/discord_message_selection.cljs` |
| Data URLs, native media and SVG rendering | `extern/discord_upload.cljs` |
| API operations and multipart submission | `extern/discord_tool_operations.cljs` |
| SDK arguments, progress and results | `extern/discord_tool_execution.cljs` |
| Parameter schemas | `law/discord_tools.cljc` |
| Tool construction and capability selection | `extern/discord_tool_catalog.cljs` |

A token comparison of all 86 original definitions found no changes to bodies
or function metadata beyond namespace qualification, cross-module helper
visibility and added docstrings. The existing REST client remains the remote
provider. The compatibility namespace contains no raw JavaScript interop.

All eight source modules and the new regression namespace pass scoped
clj-kondo with zero errors/warnings. The focused guarded run passed eight tests /
19 assertions, zero failures/errors and process exit zero; 273 compiler inputs
produced zero warnings. The tests include the previous extern Discord checks
and prove all 13 tool identifiers, capability filtering, native camel-case and
snake-case parameters, UTF-8 data URL decoding, malformed attachment refusal,
and good/bad message ordering.

The proof uses frozen eta-identity
`e0cdf3585b15101c83ef01b7bc2901652b3b54dd` through local dependency overrides.
It does not contact Discord or establish an authenticated OAuth session. The
complete backend suite and advertised production build remain separate gates
after this extraction and the concurrently prepared policy extraction.

The complete guarded backend suite then passed 1,797 tests / 8,124 assertions,
zero failures/errors and process exit zero, with 954 compiler inputs and zero
warnings. A subsequent whole-project lint correctly identified that one new
selection test called a private helper; the test now exercises the public
selection operation with an explicitly disabled REST provider. The fresh
combined memory/Discord run passed 16 tests / 53 assertions with the fatal guard,
zero failures/errors, process exit zero and zero compiler warnings. The next
complete run remains required for that test edit.
