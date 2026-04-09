(ns kms-ingestion.api.query-support
  "Query, retrieval, and answer-generation helpers for the ingestion API."
  (:require
   [clj-http.client :as http]
   [clojure.string :as str]
   [kms-ingestion.api.common :as common]
   [kms-ingestion.config :as config]))

(def role-presets
  {"workspace" ["devel"]
   "knowledge" ["devel"]
   "development" ["devel" "cephalon-hive"]
   "devsecops" ["devel" "web" "bluesky" "cephalon-hive"]
   "analyst" ["devel" "web" "bluesky"]
   "owner" ["devel" "web" "bluesky" "cephalon-hive"]
   "cto" ["devel" "web" "bluesky" "cephalon-hive"]})

(defn expand-projects
  [tenant-id role projects]
  (let [normalized-projects (->> projects (filter some?) vec)]
    (cond
      (seq normalized-projects)
      normalized-projects

      (and role (contains? role-presets role))
      (get role-presets role)

      :else
      [tenant-id])))

(defn row-path
  [row]
  (or (:source_path row)
      (:source-path row)
      (:message row)
      (:source row)
      (:id row)))

(defn call-openplanner-fts
  [{:keys [q limit project kind]}]
  (let [base-url (config/openplanner-url)
        api-key (config/openplanner-api-key)
        payload (cond-> {:q q :limit (or limit 10)}
                  project (assoc :project project)
                  kind (assoc :kind kind))
        headers (cond-> {"Content-Type" "application/json"}
                  (not (str/blank? api-key))
                  (assoc "Authorization" (str "Bearer " api-key)))
        resp (http/post
              (str base-url "/v1/search/fts")
              {:headers headers
               :body (common/clj->json payload)
               :content-type :json
               :accept :json
               :as :json
               :throw-exceptions false
               :socket-timeout 30000
               :connection-timeout 30000})]
    (if (= 200 (:status resp))
      (get-in resp [:body :rows] [])
      [])))

(defn call-openplanner-vector
  [{:keys [q limit project kind]}]
  (let [base-url (config/openplanner-url)
        api-key (config/openplanner-api-key)
        payload (cond-> {:q q :k (or limit 20)}
                  project (assoc :project project)
                  kind (assoc :kind kind))
        headers (cond-> {"Content-Type" "application/json"
                         "Accept-Encoding" "identity"}
                  (not (str/blank? api-key))
                  (assoc "Authorization" (str "Bearer " api-key)))
        resp (http/post
              (str base-url "/v1/search/vector")
              {:headers headers
               :body (common/clj->json payload)
               :content-type :json
               :accept :json
               :as :json
               :throw-exceptions false
               :socket-timeout 30000
               :connection-timeout 30000})]
    (if (= 200 (:status resp))
      (let [result (get-in resp [:body :result])
            ids (or (first (:ids result)) [])
            docs (or (first (:documents result)) [])
            metas (or (first (:metadatas result)) [])
            distances (or (first (:distances result)) [])]
        (map-indexed
         (fn [idx id]
           (let [metadata (or (nth metas idx nil) {})
                 doc (or (nth docs idx nil) "")
                 distance (nth distances idx nil)]
             {:id id
              :path (or (:sourcePath metadata)
                        (:source_path metadata)
                        (:source metadata)
                        (:path metadata)
                        (:file metadata)
                        id)
              :project (or (:project metadata) project)
              :kind (or (:kind metadata) kind)
              :snippet (if (> (count doc) 240) (subs doc 0 240) doc)
              :distance distance}))
         ids))
      [])))

(defn federated-fts
  [{:keys [tenant-id q role projects kinds limit]}]
  (let [resolved-projects (expand-projects tenant-id role projects)
        resolved-kinds (seq kinds)
        search-args (if resolved-kinds
                      (for [project resolved-projects
                            kind resolved-kinds]
                        {:q q :limit limit :project project :kind kind})
                      (for [project resolved-projects]
                        {:q q :limit limit :project project}))
        rows (mapcat call-openplanner-fts search-args)]
    {:projects resolved-projects
     :count (count rows)
     :rows (take (or limit 10) rows)}))

(defn semantic-file-search
  [{:keys [tenant-id q role projects kinds limit path-prefix]}]
  (let [resolved-projects (expand-projects tenant-id role projects)
        resolved-kinds (seq kinds)
        search-args (if resolved-kinds
                      (for [project resolved-projects
                            kind resolved-kinds]
                        {:q q :limit limit :project project :kind kind})
                      (for [project resolved-projects]
                        {:q q :limit limit :project project}))
        path-match? (fn [row]
                      (if (str/blank? path-prefix)
                        true
                        (let [current-row-path (str (:path row))
                              prefix (str path-prefix)]
                          (or (= current-row-path prefix)
                              (str/starts-with? current-row-path (str prefix "/"))))))
        normalized-fts (map (fn [row]
                              {:id (or (:id row) (str (random-uuid)))
                               :path (row-path row)
                               :project (:project row)
                               :kind (:kind row)
                               :snippet (or (:snippet row) (:text row) "")
                               :distance nil})
                            (:rows (federated-fts {:tenant-id tenant-id
                                                   :q q
                                                   :role role
                                                   :projects projects
                                                   :kinds kinds
                                                   :limit limit})))
        vector-rows (filter path-match? (mapcat call-openplanner-vector search-args))
        base-rows (if (seq vector-rows)
                    vector-rows
                    (filter path-match? normalized-fts))
        rows (->> base-rows
                  (sort-by (fn [row] (or (:distance row) 999999)))
                  (reduce (fn [acc row]
                            (if (some #(= (:path %) (:path row)) acc)
                              acc
                              (conj acc row)))
                          [])
                  (take (or limit 20)))]
    {:projects resolved-projects
     :count (count rows)
     :rows rows}))

(defn call-proxx-chat
  [{:keys [messages model system-prompt]}]
  (try
    (let [base-url (config/proxx-url)
          auth-token (config/proxx-auth-token)
          connection-timeout (config/proxx-connection-timeout-ms)
          socket-timeout (config/proxx-socket-timeout-ms)
          payload {:model (or model (config/proxx-default-model))
                   :messages messages
                   :stream false
                   :temperature 0.2}
          payload (cond-> payload
                    system-prompt (assoc :system_prompt system-prompt))
          headers (cond-> {"Content-Type" "application/json"}
                    (not (str/blank? auth-token))
                    (assoc "Authorization" (str "Bearer " auth-token)))]
      (if (str/blank? base-url)
        {:ok false
         :status 0
         :error "Proxx base URL is not configured"}
        (let [resp (http/post
                    (str base-url "/v1/chat/completions")
                    {:headers headers
                     :body (common/clj->json payload)
                     :content-type :json
                     :accept :json
                     :as :json
                     :decompress-body false
                     :throw-exceptions false
                     :socket-timeout socket-timeout
                     :connection-timeout connection-timeout})]
          (if (= 200 (:status resp))
            (let [body (:body resp)]
              {:ok true
               :text (or (get-in body [:choices 0 :message :content])
                         (get-in body [:choices 0 :text])
                         "")})
            {:ok false
             :status (:status resp)
             :error (str "Proxx returned " (:status resp) ": " (get-in resp [:body :error :message] "unknown"))}))))
    (catch Exception e
      {:ok false
       :status 0
       :error (.getMessage e)})))

(defn format-search-context
  [rows]
  (->> rows
       (map-indexed (fn [idx row]
                      (str "[" (inc idx) "] project=" (:project row)
                           " kind=" (:kind row)
                           " file=" (row-path row) "\n"
                           (or (:snippet row) (:text row) ""))))
       (str/join "\n\n")))

(defn retrieval-brief
  [projects rows]
  (if-not (seq rows)
    "No relevant workspace evidence was retrieved from OpenPlanner. Use general knowledge if needed, but say that the devel corpus did not return a match."
    (let [project-summary (->> projects (remove str/blank?) distinct (str/join ", "))
          kind-summary (->> rows (map :kind) (remove str/blank?) distinct sort (str/join ", "))]
      (str "Workspace evidence is available"
           (when-not (str/blank? project-summary)
             (str " from projects: " project-summary))
           ". Focus on the strongest supporting snippets, reconcile agreements or conflicts across them, and cite the most relevant file paths inline."
           (when-not (str/blank? kind-summary)
             (str " Retrieved material spans kinds: " kind-summary "."))))))

(defn devel-answer-system-prompt
  [{:keys [projects context-found?]}]
  (str
   "You are Knoxx, the grounded workspace assistant for the devel corpus. "
   (if context-found?
     "Use the supplied OpenPlanner context first. Synthesize across snippets instead of merely reporting that terms appear in files. Give the best grounded answer you can, then support it with the most relevant file paths and evidence. If the context is incomplete, combine it with general knowledge and clearly say where workspace evidence stops and inference begins. "
     "No relevant workspace context was found. Answer helpfully using general knowledge, clearly say that the devel corpus did not return a match, and avoid pretending you saw workspace evidence that was not retrieved. ")
   "Do not default to frequency-counting or phrasing like 'based on the context this appears in X places' unless the user explicitly asks for counts. "
   "Prefer insight, relationships, implications, and concrete next steps over enumeration. "
   "If the question is ambiguous, state your best interpretation before answering. "
   "Mention the relevant project names and file paths inline when you make claims. "
   "Structure your answer with markdown headings: ## Answer, ## Why it matters, ## Evidence, and optionally ## Gaps. "
   "Keep ## Answer concise and direct, use ## Why it matters for implications or decisions, and use ## Evidence for the specific snippets or files that support the answer. "
   "The active lakes searched were: " (str/join ", " projects) "."))

(defn build-answer-user-prompt
  [{:keys [q projects rows]}]
  (str
   "Question:\n" q
   "\n\nAnswer contract:\n"
   "- Start with ## Answer and give the best direct answer in 2-5 sentences.\n"
   "- Then add ## Why it matters with 1 short paragraph on implications, tradeoffs, or recommended action when relevant.\n"
   "- Then add ## Evidence with 2-6 bullet points citing the most relevant snippets as [1], [2], etc., plus file paths.\n"
   "- Add ## Gaps only when the retrieved evidence is missing, weak, or requires inference.\n"
   "- Do not pad with counts, frequency summaries, or 'appears in X places' language unless the question explicitly asks for counting.\n"
   "- Prefer synthesis over enumeration: explain what the retrieved context means, not just where it appeared.\n"
   "- If the evidence conflicts, say so explicitly and explain which evidence seems stronger.\n"
   "\nRetrieval brief:\n" (retrieval-brief projects rows)
   "\n\nRetrieved context:\n"
   (if (seq rows)
     (format-search-context rows)
     "No relevant workspace context was retrieved from OpenPlanner.")))

(defn grounded-summary
  [projects rows]
  (if-not (seq rows)
    "No relevant context found in the selected lakes."
    (let [by-project (frequencies (map :project rows))
          by-source (frequencies (map :source rows))
          by-kind (frequencies (map :kind rows))
          latest-ts (->> rows (map :ts) (remove nil?) sort last)
          project-summary (->> by-project (map (fn [[k v]] (str k " (" v ")"))) (str/join ", "))
          source-summary (->> by-source (map (fn [[k v]] (str k " (" v ")"))) (str/join ", "))
          kind-summary (->> by-kind (map (fn [[k v]] (str k " (" v ")"))) (str/join ", "))
          snippet-summary (->> rows (take 3) (map :snippet) (remove str/blank?) (str/join " | "))]
      (str "Found " (count rows) " result(s) across " (count projects) " lake(s): " project-summary ". "
           "Sources: " source-summary ". "
           "Kinds: " kind-summary ". "
           (when latest-ts (str "Latest event: " latest-ts ". "))
           (when (not (str/blank? snippet-summary))
             (str "Representative snippets: " snippet-summary))))))
