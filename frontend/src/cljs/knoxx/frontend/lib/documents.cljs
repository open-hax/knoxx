(ns knoxx.frontend.lib.documents
  "Documents/lakes REST calls. CLJS port of the document + database-profile
   functions in src/lib/nextApi.ts (sessionRequest: knoxx auth headers +
   a sessionStorage-persisted x-knoxx-session-id)."
  (:require [knoxx.frontend.lib.api :as lib-api]))

(def ^:private session-key "knoxx_session_id")

(defn- session-id []
  (try
    (or (.getItem js/sessionStorage session-key)
        (let [id (if (and (exists? js/crypto) (.-randomUUID js/crypto))
                   (.randomUUID js/crypto)
                   (str "sess-" (.getTime (js/Date.)) "-" (subs (.toString (js/Math.random) 36) 2 10)))]
          (.setItem js/sessionStorage session-key id)
          id))
    (catch :default _ "")))

(defn- ^:async session-request
  ([path] (await (session-request path nil)))
  ([path {:keys [method body]}]
   (let [headers (lib-api/auth-headers)
         init #js {:headers headers}]
     (.set headers "x-knoxx-session-id" (session-id))
     (when method (set! (.-method init) method))
     (when (some? body)
       (.set headers "Content-Type" "application/json")
       (set! (.-body init) (js/JSON.stringify (clj->js body))))
     (let [^js response (await (js/fetch path init))]
       (when-not (.-ok response)
         (let [text (await (.text response))]
           (throw (js/Error. (if (seq text) text
                              (str "Request failed: " (.-status response)))))))
       (js->clj (await (.json response)) :keywordize-keys true)))))

(defn fetch-documents
  "List documents in the session's active lake."
  [] (session-request "/api/documents"))

(defn ^:async upload-documents
  "Multipart upload of JS File objects, optionally auto-ingesting."
  [files auto-ingest?]
  (let [form (js/FormData.)
        headers (lib-api/auth-headers)]
    (doseq [file files] (.append form "files" file))
    (.append form "autoIngest" (str (boolean auto-ingest?)))
    (.set headers "x-knoxx-session-id" (session-id))
    (let [^js response (await (js/fetch "/api/documents/upload"
                                     #js {:method "POST" :headers headers :body form}))]
      (when-not (.-ok response)
        (throw (js/Error. "Failed to upload documents")))
      (js->clj (await (.json response)) :keywordize-keys true))))

(defn delete-document
  "Delete the document named by its encoded relative path."
  [path]
  (session-request (str "/api/documents/" (js/encodeURIComponent path)) {:method "DELETE"}))

(defn ingest-documents
  "Start ingestion using the supplied selection or full-run options."
  [options]
  (session-request "/api/documents/ingest" {:method "POST" :body (or options {})}))

(defn restart-ingestion
  "Restart or resume ingestion with an explicit fresh-run flag."
  [force-fresh?]
  (session-request "/api/documents/ingest/restart"
                   {:method "POST" :body {:forceFresh (boolean force-fresh?)}}))

(defn ingestion-progress
  "Read active ingestion and resumable checkpoint state."
  [] (session-request "/api/documents/ingestion-progress"))

(defn ingestion-history
  "Read completed ingestion runs for the active lake."
  [] (session-request "/api/documents/ingestion-history"))

(defn list-databases
  "List lake profiles and the active runtime profile."
  [] (session-request "/api/settings/databases"))

(defn create-database
  "Create a lake profile from its wire payload."
  [payload]
  (session-request "/api/settings/databases" {:method "POST" :body payload}))

(defn activate-database
  "Activate the selected lake profile."
  [id]
  (session-request "/api/settings/databases/activate" {:method "POST" :body {:id id}}))

(defn update-database
  "Patch the selected lake profile."
  [id payload]
  (session-request (str "/api/settings/databases/" (js/encodeURIComponent id))
                   {:method "PATCH" :body payload}))

(defn delete-database
  "Delete the selected lake profile."
  [id]
  (session-request (str "/api/settings/databases/" (js/encodeURIComponent id))
                   {:method "DELETE"}))

(defn make-database-private
  "Restrict the selected lake profile to this browser session."
  [id]
  (session-request (str "/api/settings/databases/" (js/encodeURIComponent id) "/make-private")
                   {:method "POST"}))
