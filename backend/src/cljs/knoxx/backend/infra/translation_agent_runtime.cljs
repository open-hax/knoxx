(ns knoxx.backend.infra.translation-agent-runtime
  "Production composition for the contract-backed `save_translation` sink.

  The sink itself takes its dependencies as data so a test can supply them. This
  namespace is the one place that knows where the real ones come from, which is
  why it exists rather than the tool handler assembling them inline: a tool
  handler that constructs its own evidence store cannot be handed a different
  one, and the drift observer needs runtime config that a tool vector does not
  carry.

  Kept separate from `infra.translation-agent-sink` for the ordinary reason —
  the sink is required by tests that must not drag a route namespace in — and
  from `infra.publication-runtime` because a translation submission arrives on
  an agent turn rather than on a reconcile request, and the two have no shared
  lifecycle."
  (:require [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.routes.translation-dispatch :as translation-dispatch]
            [knoxx.backend.infra.stores.translation-evidence-registry :as evidence-registry]
            [knoxx.backend.infra.stores.translation-split-registry :as split-registry]
            [knoxx.backend.infra.translation-agent-sink :as sink]
            [knoxx.backend.infra.translation-event-writer :as event-writer]))

(defn unavailable
  "Why the contract-backed sink cannot run, or nil when it can.

   Reported as a reason rather than as a boolean because both causes are
   operator-actionable and they call for different actions: a missing content
   root is deployment configuration, while a missing evidence store means Mongo
   is not up. Collapsed into one 'unavailable' the agent would be told to retry
   a condition no retry fixes.

   Never falls back. `stores.translation-evidence-registry` states the rule this
   follows: substituting an in-memory store would accept a translation and lose
   the binding that makes it joinable, leaving work that completed with no
   receipt anyone can find — the gate would then report it never done, forever."
  [config]
  (cond
    (empty? (str (:publication-content-root config)))
    "publication content root is not configured, so translated content has nowhere to be written"

    (nil? (evidence-registry/current))
    "translation evidence persistence is not configured, so a translation cannot be recorded"

    (nil? (split-registry/current))
    "translation split persistence is not configured, so split candidates cannot be recorded"))

(defn deps
  "The real dependencies of the contract-backed sink.

   The clock is a function rather than an instant because the receipt is stamped
   at completion, not at assembly, and a captured instant would date every
   translation in a long-lived process to when the process started."
  [config]
  (let [client (-> (openplanner-client/client config)
                   openplanner-client/assert-event-projection-repair-supported!)]
    {:content-root (:publication-content-root config)
     :evidence-store (evidence-registry/current)
     :split-store (split-registry/current)
     :digest-hex crypto/sha256-hex
     :clock (fn [] (.toISOString (js/Date.)))
     ;; The receipt is persisted before this projection is called. A failed event
     ;; write therefore fails the tool call, and the agent's equal retry repairs it
     ;; with the same stable OpenPlanner event ids.
     :emit-candidate-events! (partial event-writer/emit-candidate-events! client)
     ;; Built here rather than inside the sink, for the reason
     ;; `routes.translation.resolve-evidence-safely!` gives about its own copy:
     ;; the observer needs runtime config, and a component that constructs its own
     ;; dependencies cannot be given a different one by a test.
     :observe-source-revision (translation-dispatch/source-revision-observer! config)}))

(defn ^:async save-pair!
  "Record one agent-submitted pair, or throw the refusal the agent should read.

   Throws rather than returning a refusal because this is the tool handler's
   contract: `create-tool-obj` turns a thrown error into the agent's tool result,
   which is exactly the channel a correctable refusal belongs on. The sink keeps
   returning data so a test and a future route can classify without catching."
  [config policies pair]
  (when-let [reason (unavailable config)]
    (throw (ex-info reason {:refusal/type :sink-unavailable})))
  (let [result (await (sink/submit-pair! (deps config) policies pair))]
    (if-let [refusal (:translation/refusal result)]
      (throw (sink/refusal-error refusal))
      result)))
