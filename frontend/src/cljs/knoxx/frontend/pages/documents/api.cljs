(ns knoxx.frontend.pages.documents.api
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

(defn- session-request
  ([path] (session-request path nil))
  ([path {:keys [method body]}]
   (let [headers (lib-api/auth-headers)
         init #js {:headers headers}]
     (.set headers "x-knoxx-session-id" (session-id))
     (when method (set! (.-method init) method))
     (when (some? body)
       (.set headers "Content-Type" "application/json")
       (set! (.-body init) (js/JSON.stringify (clj->js body))))
     (-> (js/fetch path init)
         (.then (fn [^js res]
                  (if (.-ok res)
                    (.json res)
                    (-> (.text res)
                        (.then (fn [text]
                                 (throw (js/Error. (if (seq text)
                                                     text
                                                     (str "Request failed: " (.-status res)))))))))))
         (.then #(js->clj % :keywordize-keys true))))))

(defn fetch-documents [] (session-request "/api/documents"))

(defn upload-documents
  "Multipart upload of JS File objects, optionally auto-ingesting."
  [files auto-ingest?]
  (let [form (js/FormData.)
        headers (lib-api/auth-headers)]
    (doseq [file files] (.append form "files" file))
    (.append form "autoIngest" (str (boolean auto-ingest?)))
    (.set headers "x-knoxx-session-id" (session-id))
    (-> (js/fetch "/api/documents/upload" #js {:method "POST" :headers headers :body form})
        (.then (fn [^js res]
                 (if (.-ok res)
                   (.json res)
                   (throw (js/Error. "Failed to upload documents")))))
        (.then #(js->clj % :keywordize-keys true)))))

(defn delete-document [path]
  (session-request (str "/api/documents/" (js/encodeURIComponent path)) {:method "DELETE"}))

(defn ingest-documents [options]
  (session-request "/api/documents/ingest" {:method "POST" :body (or options {})}))

(defn restart-ingestion [force-fresh?]
  (session-request "/api/documents/ingest/restart"
                   {:method "POST" :body {:forceFresh (boolean force-fresh?)}}))

(defn ingestion-progress [] (session-request "/api/documents/ingestion-progress"))

(defn ingestion-history [] (session-request "/api/documents/ingestion-history"))

(defn list-databases [] (session-request "/api/settings/databases"))

(defn create-database [payload]
  (session-request "/api/settings/databases" {:method "POST" :body payload}))

(defn activate-database [id]
  (session-request "/api/settings/databases/activate" {:method "POST" :body {:id id}}))

(defn update-database [id payload]
  (session-request (str "/api/settings/databases/" (js/encodeURIComponent id))
                   {:method "PATCH" :body payload}))

(defn delete-database [id]
  (session-request (str "/api/settings/databases/" (js/encodeURIComponent id))
                   {:method "DELETE"}))

(defn make-database-private [id]
  (session-request (str "/api/settings/databases/" (js/encodeURIComponent id) "/make-private")
                   {:method "POST"}))
