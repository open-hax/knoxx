(ns knoxx.backend.extern.actor-tools
  "Agent SDK adapter for the shared actor message command."
  (:require [knoxx.backend.domain.text :as text] [knoxx.backend.domain.tools :as tools]
            [knoxx.backend.extern.actor-mailbox :as wire] [knoxx.backend.infra.actor-mailbox-commands :as commands]))
(def parameters
  [:map [:operation_id {:optional true :description "Stable caller operation ID; reuse unchanged for retries."} :string]
   [:target {:description "actor:<id>, session:<id>, conversation:<id>, self, or parent."} :string]
   [:content :string] [:mode {:optional true} [:enum "message" "follow-up" "steer" "event" "inbox-only"]]
   [:target_type {:optional true} :string] [:conversation_id {:optional true} :string]
   [:session_id {:optional true} :string] [:run_id {:optional true} :string] [:metadata_json {:optional true} :string]])
(defonce ^:private lineage-bindings (js/WeakMap.))
(defn- execute [context lineage]
  (^:async fn [runtime config id params a b c]
    (tools/maybe-tool-update! (some #(when (fn? %) %) [a b c]) "Admitting actor message…")
    (let [request (wire/decode-send id params lineage) result (await (commands/send! runtime config context request))]
      (text/tool-text-result "Actor message delivery is durably recorded."
                             (assoc result :tool "actors.send-message" :mailbox_id (get-in result [:entry :mailbox/id]) :mailbox_durable true)))))
(defn bind-lineage!
  "Capture this session's coordinates in its tool closure, never global mutable context."
  [tool lineage] (when-let [bind! (.get lineage-bindings tool)] (bind! lineage)) tool)
(defn- make-tool [runtime config context]
  (let [tool (tools/create-tool-obj "actors.send-message" "Actors Send Message"
                                   "Send a durable message to an actor or conversation."
                                   "Use the same tenant-scoped delivery command as the human mailbox."
                                   ["inbox-only stores a message for later reading; follow-up and steer require a live conversation."
                                    "A failed durable receipt is reported explicitly; do not assume an unconfirmed attempt was not delivered."]
                                   parameters (execute context {}) runtime config)]
    (.set lineage-bindings tool (fn [lineage] (aset tool "execute" (partial (execute context lineage) runtime config)))) tool))
(defn create-tools
  "Advertise according to the captured policy; execution revalidates current authority."
  ([runtime config] (create-tools runtime config nil))
  ([runtime config context] (clj->js (if (and context (commands/allowed? context)) [(make-tool runtime config context)] []))))
