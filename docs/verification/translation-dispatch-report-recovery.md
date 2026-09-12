# Translation dispatch report ownership

`law.translation-dispatch` combined new-attempt admission with worker status,
ambiguous-send recovery, source validation and receipt construction. The file
and three functions still exceeded the configured size limits after the scope
repair. Worker report interpretation now lives in
`law.translation-dispatch-report`; the existing facade owns admission and
retains every public name.

The facade is 385 lines and the report owner is 365 lines. All receipt arities
forward explicitly, preserving native ClojureScript invocation. The report's
output-revision function receives the original dispatch validator, so moving
it does not change the named error or its data. There are no moved `defonce`
values. Documentation was shortened where it repeated the same premise.

A reader-based structural comparison against the pre-extraction source found
**45 unchanged implementation bodies** after removing docstrings and accounting
for the two schema qualifications: 18 report definitions and 27 facade
implementations. Every former public definition remains available. The one
intentional internal signature change is the injected validator described
above; the public output-revision arity is unchanged. The comparison normalized
reader-generated anonymous-function IDs and compared regular expressions by
pattern, rather than treating fresh reader objects as code changes.

Full backend07 compiled 1,028 inputs with zero warnings and confirmed all 872
captured source hashes stayed unchanged. Its guarded Node run then stopped on
an actual unhandled rejection in `translation-agent-content-test`: the fixture
created a new dispatch record without membership. The constructor now requires
that coordinate; the fixture supplies an explicit member. Its temporary files
now use the named fixture boundary and are removed in `finally`, including
failure paths. No assertion was removed and no membership default was added.
An audit of the other direct constructors found explicit membership or the
already verified shared dispatch fixture.

The report source, facade, repaired fixture and focused compiler all pass the
seven optional lint rules and the existing size limits with **zero findings**.
No threshold changed. Native and subsequent complete-suite results are recorded
below after execution; a passing compiler alone is not the test result.

The focused native proof passed **36 tests / 240 assertions**, zero failures and
errors, with the fatal asynchronous guard and actual exit zero. Its independent
build compiled **544 inputs, 40 compiled, zero warnings** in 13.96 seconds.
It covers existing report laws, explicit receipt forwarding, the complete
scoped queue/turn/Clio path, runtime catalog flags, and immutable content writes
with the repaired fixture. It used frozen eta identity initialization
`fc3b6a09c6cd90ca200023cbc1fc54ec57a630a0`. Full08 is the subsequent combined
backend gate; full07 remains a correctly rejected diagnostic run.
