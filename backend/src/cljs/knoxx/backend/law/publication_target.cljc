(ns knoxx.backend.law.publication-target
  "Contracts for resource-declared publication targets.

   A declaration is data: it names a stable target and the kind-specific
   configuration needed by runtime composition. It never embeds an adapter,
   transport handle, or mutable state. Locale admission is deliberately absent:
   `knoxx-publication-locale-catalog` owns that separate policy."
  (:require [malli.core :as m]
            [malli.error :as me]))

(def PublicationTargetDeclaration
  "A resource-shaped declaration selecting one runtime publication adapter.

   `:publication-target/id` is the identity a publication intent names;
   `:publication-target/kind` chooses a Knoxx-owned factory; and
   `:publication-target/config` is opaque, kind-specific data. `enabled?` is
   explicit so an operator cannot make a target unavailable by omission."
  [:map
   [:publication-target/id qualified-keyword?]
   [:publication-target/kind keyword?]
   [:publication-target/config :map]
   [:publication-target/enabled? boolean?]])

(defn assert-declarations!
  "Return declarations when every declaration is lawful and every target id is
   unique; otherwise throw before runtime composition can create an adapter."
  [declarations]
  (let [declarations (vec declarations)]
    (doseq [declaration declarations]
      (when-not (m/validate PublicationTargetDeclaration declaration)
        (throw (ex-info "Publication target declaration contract violation"
                        {:contract :publication/target-declaration
                         :errors (me/humanize
                                  (m/explain PublicationTargetDeclaration declaration))}))))
    (let [ids (mapv :publication-target/id declarations)
          duplicate-ids (->> ids frequencies (keep (fn [[id n]]
                                                       (when (< 1 n) id))) sort vec)]
      (when (seq duplicate-ids)
        (throw (ex-info "Duplicate publication target declarations"
                        {:publication-target/ids duplicate-ids})))
      declarations)))
