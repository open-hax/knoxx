(ns knoxx.backend.runtime.contract-validator
  (:require [malli.core :as m]
            [malli.error :as me]))

(def Contract
  "Minimal contract schema for EDN contracts stored on disk.

   We intentionally allow extra keys so the contract language can evolve
   without forcing a compiler migration."
  [:map
   [:contract/id string?]
   [:contract/kind keyword?]
   [:contract/version {:optional true} int?]
   [:enabled {:optional true} boolean?]])

(defn- collect-humanized-errors
  [prefix value]
  (cond
    (nil? value) []
    (string? value) [{:path prefix :message value}]
    (vector? value) (mapcat #(collect-humanized-errors prefix %) value)
    (sequential? value) (mapcat #(collect-humanized-errors prefix %) value)
    (map? value) (mapcat (fn [[k v]]
                           (collect-humanized-errors (conj prefix (name k)) v))
                         value)
    :else [{:path prefix :message (pr-str value)}]))

(defn validate
  "Validate a parsed contract map.

   Returns:
   - {:ok true :value contract :errors []}
   - {:ok false :value contract :errors [{:path [...] :message \"...\"} ...]}"
  [value]
  (let [ok? (m/validate Contract value)
        explained (when-not ok? (m/explain Contract value))
        errors (if explained
                 (->> (collect-humanized-errors [] (me/humanize explained))
                      (map (fn [err]
                             (update err :path (fn [p] (mapv str p)))))
                      vec)
                 [])]
    {:ok ok?
     :value value
     :errors errors}))
