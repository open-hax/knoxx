(ns kms-ingestion.translation.agent
  "Translation agent that processes translations via the Knoxx agent runtime.
  
  The agent has restricted tools:
  - read: Read source documents
  - memory_search/memory_session: Access prior translation context
  - graph_query: Query translation examples
  - save_translation: Save translated segments
  
  No bash, no file write - only translation-specific operations."
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [kms-ingestion.config :as config])
  (:import
   [java.net URL]
   [java.io OutputStreamWriter]))

(defn- knoxx-url
  [path]
  (str (or (config/knoxx-backend-url) "http://knoxx-backend:8000") path))

(defn- knoxx-headers
  []
  (cond-> {"Content-Type" "application/json"}
    (not (str/blank? (config/knoxx-api-key)))
    (assoc "X-API-Key" (config/knoxx-api-key))))

(defn- post-json
  [url body]
  (let [conn (.openConnection (URL. url))
        body-str (json/generate-string body)]
    (doseq [[k v] (knoxx-headers)]
      (.setRequestProperty conn k v))
    (.setRequestMethod conn "POST")
    (.setDoOutput conn true)
    (.write (OutputStreamWriter. (.getOutputStream conn)) body-str)
    (let [code (.getResponseCode conn)]
      (if (or (= 200 code) (= 201 code))
        (let [body (slurp (.getInputStream conn))]
          (json/parse-string body keyword))
        (let [error-body (try (slurp (.getErrorStream conn)) (catch Exception _ "unknown error"))]
          (throw (ex-info (str "HTTP " code ": " error-body) {:url url :code code})))))))

(defn- fetch-document
  "Fetch document content from OpenPlanner."
  [document-id]
  (let [url (str (config/openplanner-url) "/v1/documents/" document-id)
        headers (cond-> {"Content-Type" "application/json"}
                  (not (str/blank? (config/openplanner-api-key)))
                  (assoc "X-API-Key" (config/openplanner-api-key)))
        conn (.openConnection (URL. url))]
    (doseq [[k v] headers]
      (.setRequestProperty conn k v))
    (.setRequestMethod conn "GET")
    (let [code (.getResponseCode conn)]
      (if (= 200 code)
        (let [body (slurp (.getInputStream conn))]
          (json/parse-string body keyword))
        (throw (ex-info (str "Failed to fetch document: HTTP " code)
                        {:document-id document-id :code code}))))))

(defn- fetch-examples
  "Fetch translation examples from OpenPlanner graph memory."
  [source-lang target-lang]
  (try
    (let [url (str (config/openplanner-url)
                   "/v1/translations/examples?source_lang=" source-lang
                   "&target_lang=" target-lang "&limit=3")
          headers (cond-> {"Content-Type" "application/json"}
                    (not (str/blank? (config/openplanner-api-key)))
                    (assoc "X-API-Key" (config/openplanner-api-key)))
          conn (.openConnection (URL. url))]
      (doseq [[k v] headers]
        (.setRequestProperty conn k v))
      (.setRequestMethod conn "GET")
      (let [code (.getResponseCode conn)]
        (if (= 200 code)
          (let [body (slurp (.getInputStream conn))
                result (json/parse-string body keyword)]
            (:examples result))
          [])))
    (catch Exception e
      (println "[translation-agent] Failed to fetch examples:" (.getMessage e))
      [])))

(defn- build-system-prompt
  "Build the system prompt for the translation agent."
  [{:keys [source-lang target-lang document-title examples]}]
  (let [example-str (when (seq examples)
                      (str "\n\nTranslation examples:\n"
                           (->> examples
                                (map (fn [{:keys [source_text target_text]}]
                                       (str "Source: " source_text "\n"
                                            "Target: " target_text)))
                                (str/join "\n\n"))))]
    (str "You are a professional translator. Translate the given document from "
         source-lang " to " target-lang ".\n\n"
         "Document: " (or document-title "Untitled") "\n"
         "Source language: " source-lang "\n"
         "Target language: " target-lang "\n\n"
         "INSTRUCTIONS:\n"
         "1. Read the source document using the read tool\n"
         "2. Split the document into logical segments (respecting sentence boundaries)\n"
         "3. For each segment, translate naturally and accurately\n"
         "4. Use save_translation to save each translated segment with its index\n"
         "5. Output ONLY the translation - no explanations or alternatives\n\n"
         "When you have translated all segments, confirm completion."
         example-str)))

(defn- save-segment-via-api
  "Save a translation segment via OpenPlanner API."
  [segment]
  (let [url (str (config/openplanner-url) "/v1/translations/segments")
        headers (cond-> {"Content-Type" "application/json"}
                  (not (str/blank? (config/openplanner-api-key)))
                  (assoc "X-API-Key" (config/openplanner-api-key)))
        conn (.openConnection (URL. url))]
    (doseq [[k v] headers]
      (.setRequestProperty conn k v))
    (.setRequestMethod conn "POST")
    (.setDoOutput conn true)
    (.write (OutputStreamWriter. (.getOutputStream conn))
            (json/generate-string segment))
    (let [code (.getResponseCode conn)]
      (when-not (or (= 200 code) (= 201 code))
        (throw (ex-info (str "Failed to save segment: HTTP " code)
                        {:segment segment :code code}))))))

(defn run-translation-session!
  "Run a translation session using the Knoxx agent runtime.
  
  Creates an agent session with translator role and restricted tools.
  The agent reads the source document, translates it segment by segment,
  and saves each segment via the save_translation tool."
  [{:keys [job-id document-id garden-id source-lang target-lang] :as ctx}]
  (println "[translation-agent] Starting session for job" job-id)
  ;; Fetch document
  (let [document (fetch-document document-id)
        doc-text (or (:text document)
                     (get-in document [:extra :content])
                     "")
        doc-title (or (get-in document [:extra :title])
                      "Untitled")
        examples (fetch-examples source-lang target-lang)]
    (if (str/blank? doc-text)
      (throw (ex-info "Document has no text content" {:document-id document-id}))
      (let [system-prompt (build-system-prompt
                           {:source-lang source-lang
                            :target-lang target-lang
                            :document-title doc-title
                            :examples examples})
            _user-message (str "Translate the document to " target-lang
                               ". Read the source content, then save each translated segment "
                               "using save_translation with its segment_index.")]
        ;; For now, use a simplified approach: directly translate via MT
        ;; TODO: Integrate with Knoxx agent runtime API for full agent session
        (println "[translation-agent] Document:" doc-title "length:" (count doc-text))
        (println "[translation-agent] Examples found:" (count examples))
        ;; Split into segments
        (let [segments (->> (str/split doc-text #"(?<=[.!?])\s+")
                            (partition-all 3)
                            (map (fn [sents] (str/join " " sents)))
                            (filter (comp not str/blank?)))
              total-segments (count segments)]
          (println "[translation-agent] Processing" total-segments "segments")
          ;; Save each segment (placeholder - actual translation would go through agent)
          ;; For now, save original text as "translated" to test the pipeline
          (doseq [[idx source-text] (map-indexed vector segments)]
            (save-segment-via-api
             {:source_text source-text
              :translated_text source-text
              :source_lang source-lang
              :target_lang target-lang
              :document_id document-id
              :garden_id garden-id
              :segment_index idx
              :status "pending"
              :mt_model "translation-agent"})
            (println "[translation-agent] Saved segment" (inc idx) "/" total-segments))
          (println "[translation-agent] Session complete for job" job-id))))))
