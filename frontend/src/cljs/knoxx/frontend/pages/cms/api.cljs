(ns knoxx.frontend.pages.cms.api
  "Authenticated resource commands and live publication transport."
  (:require [knoxx.frontend.lib.api :as api]
            [knoxx.frontend.pages.cms.logic :as logic]))

(defn document-path
  "Encode the complete qualified document identity as one route parameter."
  [document]
  (str "/api/publications/documents/" (js/encodeURIComponent document)))

(defn ^:async list-documents
  "Load the actual document and garden topology."
  []
  (logic/normalize-inventory (await (api/request "/api/publications/documents"))))

(defn ^:async get-review
  "Validate the source projection before the editor receives it."
  [document]
  (logic/assert-snapshot! (await (api/request (str (document-path document) "/review")))))

(defn- checked-command-result [result]
  (logic/assert-snapshot! (:review result))
  result)

(defn ^:async create-document
  "Create real source and private publication resources with an idempotent operation."
  [payload]
  (checked-command-result
   (await (api/request "/api/publications/documents" {:method "POST" :body payload}))))

(defn ^:async save-source
  "Write source against its expected revision, without editing title metadata."
  [document payload]
  (checked-command-result
   (await (api/request (str (document-path document) "/source")
                       {:method "PATCH" :body payload}))))

(defn ^:async submit-review
  "Append one revision- and history-bound source review command."
  [document payload]
  (checked-command-result
   (await (api/request (str (document-path document) "/review")
                       {:method "POST" :body payload}))))

(defn assist
  "Ask the configured writing model using exact source and accepted writing lessons."
  [document payload]
  (api/request (str (document-path document) "/assist") {:method "POST" :body payload}))

(defn ^:async get-publications
  "Read truthful desired and observed state for every selected placement."
  [document]
  (logic/assert-publications!
   (await (api/request (str (document-path document) "/publications")))))

(defn ^:async publish
  "Request one source-bound placement; a successful response may still carry blockers."
  [document payload]
  (checked-command-result
   (await (api/request (str (document-path document) "/publish")
                       {:method "POST" :body payload}))))

(defn decode-change
  "Accept named document changes and the bounded-queue global refresh marker."
  [data]
  (try
    (let [value (js->clj (js/JSON.parse data) :keywordize-keys true)]
      (when (and (map? value) (contains? value :document)
                 (or (nil? (:document value)) (string? (:document value))))
        value))
    (catch :default _ nil)))

(defn subscribe!
  "Refresh on authenticated SSE changes and reconnect; close the stream on cleanup."
  [on-change]
  (if (exists? js/EventSource)
    (let [stream (js/EventSource. "/api/publications/changes" #js {:withCredentials true})
          handler (fn [event]
                    (when-let [change (decode-change (.-data ^js event))] (on-change change)))]
      (.addEventListener stream "publication-changed" handler)
      (set! (.-onopen stream) #(on-change {:document nil}))
      (fn [] (.removeEventListener stream "publication-changed" handler) (.close stream)))
    (fn [] nil)))
