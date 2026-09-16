(ns knoxx.frontend.law.migration-worker
  "Admission contract for worker sources accounted for by the migration census.")

(defn assert-cljs-source!
  "Require explicit inventory support before CLJS supplies browser worker inputs."
  [{:keys [path references] :as facts}]
  (when (seq references)
    (throw (ex-info "CLJS browser workers require explicit migration inventory support"
                    {:path path :references references})))
  facts)

(defn assert-reference!
  "Reject browser worker values escaping the directly inspected source-call grammar."
  [{:keys [path kind usage] :as facts}]
  (when-not (contains? #{:constructor-call :registration-call :query-call :type-query} usage)
    (throw (ex-info "Browser worker reference is outside the supported migration grammar"
                    {:path path :kind kind})))
  facts)

(defn assert-source!
  "Require browser workers to use the dependency URL grammar inspected by the census."
  [{:keys [path kind governed-url?] :as facts}]
  (when-not governed-url?
    (throw (ex-info "Worker source is outside the supported migration URL grammar"
                    {:path path :kind kind})))
  facts)
