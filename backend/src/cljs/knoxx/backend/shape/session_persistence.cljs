(ns knoxx.backend.shape.session-persistence
  "Shared persistence contract for knoxx session state.

   Both Redis and OpenPlanner implement ISessionStore.
   The composite driver writes to both and verifies they agree."
  (:require [malli.core :as m]
            [malli.error :as me]))

(def RunStatus
  [:enum "queued" "running" "completed" "failed" "waiting_input" "cancelled"])

(def ToolReceiptStatus
  "Runtime status vocabulary accepts current stream values plus legacy
   trace/persistence values during migration. Prefer running/completed/failed at
   runtime; translate done/error only at compatibility boundaries."
  [:enum "running" "completed" "failed" "done" "error"])

(def TraceBlockStatus
  [:enum "streaming" "done" "error" "completed" "failed"])

(def ToolReceipt
  [:map {:closed false}
   [:id :string]
   [:tool_name :string]
   [:status ToolReceiptStatus]
   [:started_at {:optional true} :string]
   [:ended_at {:optional true} :string]
   [:input_preview {:optional true} [:maybe :string]]
   [:result_preview {:optional true} [:maybe :string]]])

(def TraceBlock
  [:map {:closed false}
   [:id :string]
   [:kind [:enum :tool_call :text :reasoning :agent_message]]
   [:status TraceBlockStatus]
   [:at {:optional true} :string]])

(def Message
  [:map {:closed false}
   [:role [:enum "user" "assistant" "system"]]
   [:content :string]])

(def KnoxxRun
  [:map {:closed false}
   [:run_id :string]
   [:session_id :string]
   [:conversation_id :string]
   [:status RunStatus]
   [:created_at :string]
   [:updated_at :string]
   [:model {:optional true} [:maybe :string]]
   [:messages {:optional true} [:vector Message]]
   [:tool_receipts {:optional true} [:vector ToolReceipt]]
   [:trace_blocks {:optional true} [:vector TraceBlock]]
   [:answer {:optional true} [:maybe :string]]
   [:reasoning {:optional true} [:maybe :string]]
   [:error {:optional true} [:maybe :string]]
   [:has_active_stream {:optional true} :boolean]
   [:org_id {:optional true} [:maybe :string]]
   [:user_id {:optional true} [:maybe :string]]])

(defn valid-run?
  [run]
  (m/validate KnoxxRun run))

(defn explain-run
  [run]
  (me/humanize (m/explain KnoxxRun run)))

(defn assert-run!
  [run ctx]
  (when-not (valid-run? run)
    (throw (ex-info (str ctx ": invalid KnoxxRun")
                    {:errors (explain-run run)
                     :run (select-keys run [:run_id :session_id :status])}))))

(defprotocol ISessionStore
  (put-run! [store run]
    "Persist a full KnoxxRun. Validates against KnoxxRun schema.
     Resolves to the stored run map.")

  (get-run [store run-id]
    "Fetch a KnoxxRun by run-id.
     Resolves to a KnoxxRun map or nil.")

  (patch-run! [store run-id patch]
    "Merge patch into the stored run. patch must not violate KnoxxRun invariants.
     Resolves to the updated KnoxxRun.")

  (list-active-runs [store session-id]
    "List KnoxxRuns for session-id where status ∈ #{running queued waiting_input}.
     Resolves to a vector of KnoxxRun maps.")

  (complete-run! [store run-id opts]
    "Finalize a run: set status, answer, error, trace_blocks.
     opts: {:status :answer :error :trace_blocks :messages}
     Resolves to the finalized KnoxxRun.")

  (delete-run! [store run-id]
    "Remove a run from this store. Idempotent.
     Resolves to true."))