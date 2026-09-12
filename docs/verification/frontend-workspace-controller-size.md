# Chat workspace state and session ownership

The chat controller now composes the existing configuration/state hook, action
factories and effects. It selects explicit typed state fields for each consumer
and its public return value. The original state initialization block, hook order,
defaults and ref writes were retained verbatim. An independent TypeScript syntax
tree comparison confirms that all nine selected consumer/public property sets
match the prior controller, with no duplicate or additional fields.

Session page merging and ordering now live with persisted session storage. Actor
visibility policy lives with chat configuration. Model resume policy and persisted
chat preferences live with the settings panel. The action and controller input
types live with the existing context/workspace contracts; compatibility exports
remain available. Effect dependency lists still depend on individual state values.

The controller shrank from 757 to 389 lines and workspace actions from 559 to 373.
All seven changed source/test files are below the unchanged 400-line warning
threshold. The remaining frontend size debt is four large pages and GraphExplorer;
repository-wide lint remains failing until those and the backend size warnings
are resolved.

Three new hook regressions cover persisted preference restoration, stable refs
across state updates, exact selected fields and setter identity, and unavailable
browser storage. The first generic key-selection signature failed typecheck with
TS7053; using a key union constrained to the state contract fixed the indexing
without weakening types or casting the result through any.

Focused verification passes **5 files / 37 tests**. The complete legacy frontend
suite passes **45 files / 274 executed tests / 0 failures**, with **41 existing
TODO cases explicitly unexecuted**. Full TypeScript checking and isolated legacy
emission also pass. No served production output was changed during these gates.
