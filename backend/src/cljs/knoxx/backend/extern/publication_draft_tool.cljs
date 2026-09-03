(ns knoxx.backend.extern.publication-draft-tool
  "Wire adapter for the publication post-drafter's save tool.

  Eta-mu supplies a native JavaScript arguments object. This namespace owns
  decoding and validation so the publication infra receives only plain CLJS
  data."
  (:require [malli.core :as m]))

(def save-draft-params
  [:map {:closed true}
   [:title {:optional true
            :description "Optional title. When absent or blank, Knoxx derives it from the first Markdown ATX heading or the pinned source document."}
    :string]
   [:content {:description "Required complete Markdown body for the crafted draft post."}
    :string]])

(defn decode-save-draft-params!
  "Decode one raw eta-mu arguments object into a validated CLJS map."
  [raw-params]
  (let [params (cond
                 (nil? raw-params) {}
                 (map? raw-params) raw-params
                 :else (js->clj raw-params :keywordize-keys true))]
    (when-not (m/validate save-draft-params params)
      (throw (ex-info "save_publication_draft received invalid tool arguments"
                      {:status 400
                       :code :publication-draft-tool-params-invalid
                       :explain (m/explain save-draft-params params)})))
    params))
