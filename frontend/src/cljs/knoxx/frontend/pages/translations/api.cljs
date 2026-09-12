(ns knoxx.frontend.pages.translations.api
  "Translation review REST calls. CLJS port of the translation functions
   in src/lib/api/common.ts, the pipeline config in
   src/lib/api/openplanner.ts and listProxxModels in src/lib/api/runtime.ts."
  (:require [knoxx.frontend.lib.api :as api]))

(defn list-documents
  "Read translation summaries for one project and optional target locale."
  [{:keys [project target-lang]}]
  (let [q (js/URLSearchParams.)]
    (.set q "project" project)
    (when (seq target-lang) (.set q "target_lang" target-lang))
    (api/request (str "/api/translations/documents?" (.toString q)))))

(defn- wire-scope
  [{:keys [project garden-id]}]
  (cond-> {}
    (seq project) (assoc :project project)
    (seq garden-id) (assoc :garden_id garden-id)))

(defn- document-path
  [document-id target-lang]
  (str "/api/translations/documents/"
       (js/encodeURIComponent document-id) "/"
       (js/encodeURIComponent target-lang)))

(defn get-document
  "Load one legacy split document under its project and garden coordinates."
  ([document-id target-lang]
   (get-document document-id target-lang nil))
  ([document-id target-lang scope]
   (let [q (js/URLSearchParams.)
         {:keys [project garden_id]} (wire-scope scope)]
     (when project (.set q "project" project))
     (when garden_id (.set q "garden_id" garden_id))
     (api/request (str (document-path document-id target-lang)
                       (when (seq (.toString q))
                         (str "?" (.toString q))))))))

(defn review-document
  "Apply a document-wide legacy split review under exact row scope."
  ([document-id target-lang payload]
   (review-document document-id target-lang nil payload))
  ([document-id target-lang scope payload]
   (api/request (str (document-path document-id target-lang) "/review")
                {:method "POST"
                 :body (merge payload (wire-scope scope))})))

(defn list-publication-reviews
  "Read authoritative publication translation work and review candidates."
  []
  (api/request "/api/publications/translations/reviews"))

(defn ^:async submit-publication-split-review
  "Record one candidate-set-bound split verdict for the authenticated reviewer."
  [payload]
  (await (api/request "/api/publications/translations/reviews"
                      {:method "POST" :body payload})))

(defn ^:async submit-publication-bulk-review
  "Apply one evaluation and verdict to every persisted split in a candidate set."
  [payload]
  (await (api/request "/api/publications/translations/reviews/bulk"
                      {:method "POST" :body payload})))

(defn dispatch-publication-translation
  "Ask Knoxx to dispatch exactly the resource work row the reviewer selected.

   Publication identity stays server-resolved.  The client does not send a
   revision or a retry flag: the resource snapshot decides the concrete work,
   and durable dispatch evidence decides whether this is a first attempt,
   in-flight duplicate, terminal duplicate, or lawful retry."
  [publication-id]
  (api/request "/api/publications/translations/dispatch"
               {:method "POST" :body {:publication publication-id}}))

(defn approve-publication-translation
  "Submit the exact immutable publication approval coordinates."
  [payload]
  (api/request "/api/publications/translations/approvals"
               {:method "POST" :body payload}))

(defn reconcile-publication
  "Request reconciliation of one explicit publication placement."
  [publication-id]
  (api/request "/api/publications/reconcile"
               {:method "POST"
                :body {:publicationId publication-id}}))

(defn submit-label
  "Apply a split judgment, carrying the selected row's project and garden."
  ([segment-id payload]
   (submit-label segment-id nil payload))
  ([segment-id scope payload]
   (api/request (str "/api/translations/segments/"
                     (js/encodeURIComponent segment-id) "/labels")
                {:method "POST"
                 :body (merge payload (wire-scope scope))})))

(defn get-manifest
  "Read the export manifest for an encoded project identifier."
  [project]
  (api/request (str "/api/translations/export/manifest?project="
                    (js/encodeURIComponent project))))

(defn sft-export
  "Download translation training rows with accepted corrections included."
  [{:keys [project target-lang]}]
  (let [q (js/URLSearchParams.)]
    (.set q "project" project)
    (when (seq target-lang) (.set q "target_lang" target-lang))
    (.set q "include_corrected" "true")
    (api/request-text (str "/api/translations/export/sft?" (.toString q)))))

;; Knoxx-owned translation configuration. The response is the wire config
;; itself — `{:model :source-locale :default-review}` — not wrapped under
;; `:config`, so there is nothing to unwrap. `:model` is a catalog model id, and
;; `:model` is also the unqualified wire key the backend contract declares,
;; because `api/request` serializes with `clj->js` and would erase a namespace.
(defn ^:async pipeline-config
  "Read the native translation pipeline configuration wire."
  []
  (await (api/request "/api/translations/config")))

(defn ^:async update-pipeline-config
  "Patch the pipeline model using the existing unqualified model key."
  [model]
  (await (api/request "/api/translations/config"
                      {:method "PATCH" :body {:model model}})))

(defn ^:async list-proxx-models
  "List available model entries sorted by their catalog identifier."
  []
  (let [response (await (api/request "/api/proxx/models"))]
    (vec (sort-by :id (:models response)))))
