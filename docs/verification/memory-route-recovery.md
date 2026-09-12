# Memory route extraction

The 818-line route module exceeded the existing size contract. Its public
facade now owns registration while three named native boundaries own the cache,
provider pagination, and response enrichment. Every previous public export is
retained, including the cache and pagination entry points used by tests.

| Owner | Responsibility |
| --- | --- |
| `extern/memory_session_cache.cljs` | Scoped canonical cache key, clock, cache state, admission and concurrent read coalescing |
| `extern/memory_session_pages.cljs` | Provider pagination and authorized actor/contract filtering |
| `extern/memory_session_view.cljs` | Live session rows, title warming, native response enrichment |
| `infra/routes/memory.cljs` | Route registration and compatibility exports |

A token comparison found all 67 original definitions unchanged apart from
namespace qualification, helper visibility and docstrings. An independent
review confirmed all 31 public exports, an acyclic dependency graph, cache
state co-located with its readers/writers, and no broken override seam in the
current callers. The special injected-versus-default history reader comparison
remains intact. No lint rule or threshold changed.

The existing cache and pagination tests run alongside a real Fastify regression.
That regression refuses an anonymous request before any provider read, then
shows that verified tenant scope replaces both ambient configuration and a
caller-supplied tenant query. The actual route, in-memory cache and response
enrichment execute; persistent storage and identity resolution are explicit
finite fixtures. This does not claim a real identity login or durable cache.

The first native run expected an upstream page size of two, but the existing
contract has a minimum of ten. The assertion now uses that actual contract.
Self-review also caught a test double with one arity for a two-arity active
session reader. Both arities are now present, and an explicit call count prevents
the route's fallback from concealing an unintended early failure.

The fresh combined memory/Discord proof passed 16 tests / 53 assertions with
the fatal guard, zero failures/errors and process exit zero. Compilation covered
312 inputs with zero warnings. Changed source and tests pass scoped clj-kondo
with zero errors/warnings. The proof uses frozen eta-identity
`e0cdf3585b15101c83ef01b7bc2901652b3b54dd` through local dependency overrides.
The next complete backend run and production build remain separate gates.
