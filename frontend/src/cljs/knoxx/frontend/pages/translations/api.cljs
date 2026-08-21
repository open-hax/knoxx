(ns knoxx.frontend.pages.translations.api
  "Translation review REST calls. CLJS port of the translation functions
   in src/lib/api/common.ts, the pipeline config in
   src/lib/api/openplanner.ts and listProxxModels in src/lib/api/runtime.ts."
  (:require [knoxx.frontend.lib.api :as api]))

(defn list-documents [{:keys [project target-lang]}]
  (let [q (js/URLSearchParams.)]
    (.set q "project" project)
    (when (seq target-lang) (.set q "target_lang" target-lang))
    (api/request (str "/api/translations/documents?" (.toString q)))))

(defn get-document [document-id target-lang]
  (api/request (str "/api/translations/documents/"
                    (js/encodeURIComponent document-id) "/"
                    (js/encodeURIComponent target-lang))))

(defn review-document [document-id target-lang payload]
  (api/request (str "/api/translations/documents/"
                    (js/encodeURIComponent document-id) "/"
                    (js/encodeURIComponent target-lang) "/review")
               {:method "POST" :body payload}))

(defn submit-label [segment-id payload]
  (api/request (str "/api/translations/segments/"
                    (js/encodeURIComponent segment-id) "/labels")
               {:method "POST" :body payload}))

(defn get-manifest [project]
  (api/request (str "/api/translations/export/manifest?project="
                    (js/encodeURIComponent project))))

(defn sft-export [{:keys [project target-lang]}]
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
(defn ^:async pipeline-config []
  (await (api/request "/api/translations/config")))

(defn ^:async update-pipeline-config [model]
  (await (api/request "/api/translations/config"
                      {:method "PATCH" :body {:model model}})))

(defn list-proxx-models []
  (-> (api/request "/api/proxx/models")
      (.then #(vec (sort-by :id (:models %))))))
