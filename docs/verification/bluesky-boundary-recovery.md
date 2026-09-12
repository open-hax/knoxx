# Bluesky tool boundary extraction

The former 927-line tool module mixed schemas, native SDK arguments, UTF-8
offsets, remote operations, display formatting and tool selection. It exceeded
the existing file-size contract. The extraction keeps every previous public
export available at `knoxx.backend.domain.bluesky.bluesky`; callers keep their
existing names and arities.

| Responsibility | Owner |
| --- | --- |
| Parameter schemas | `law/bluesky_tools.cljc` |
| URI and display projections | `shape/bluesky.cljs` |
| Native UTF-8 offsets and regex results | `extern/bluesky_facets.cljs` |
| Remote operations, native media and clock | `extern/bluesky_operations.cljs` |
| SDK parameter decoding and result callbacks | `extern/bluesky_tool_execution.cljs` |
| Native tool objects and capability selection | `extern/bluesky_tool_catalog.cljs` |

All seven source modules pass scoped clj-kondo with zero errors and warnings.
No thresholds or warning levels were relaxed. The compatibility namespace owns
no raw JavaScript interop. The existing client protocol and remote provider are
unchanged by this extraction.

The boundary regression checks all 19 public tool identifiers, scoped policy
selection, UTF-8 offsets after accented and supplementary characters, native
publish parameters and progress callbacks, and blank-input refusal before the
remote operation. These checks use a finite remote-operation fixture; they do
not claim a real authenticated Bluesky request or OAuth login.

The focused run, including the repaired OpenPlanner fixtures, passed 24 tests /
62 assertions with the fatal asynchronous guard and zero failures/errors. Its
292 compiler inputs produced zero warnings. The JS boundary check also passes.
These checks use eta-identity `e0cdf3585b15101c83ef01b7bc2901652b3b54dd` through
explicit local dependency overrides. The subsequent complete guarded backend
run passed 1,786 tests / 8,083 assertions with zero failures/errors and process
exit zero; its 931 compiler inputs produced zero warnings.
