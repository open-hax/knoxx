(ns knoxx.backend.infra.stores.session-titles
  (:require [clojure.string :as str]
            [knoxx.backend.extern.row-extra :as row-extra]
            [knoxx.backend.extern.proxx :as proxx]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.stores.mongo-session-titles :as mongo-titles]
            [knoxx.backend.domain.time :as time]
            [knoxx.backend.domain.text :as text]))

(defonce session-titles* (atom {}))
(defonce session-title-promises* (atom {}))
(defonce session-title-generation-tail* (atom (js/Promise.resolve nil)))
(defonce session-title-backfill* (atom {:active false
                                        :processed 0
                                        :total 0
                                        :failed 0
                                        :force false
                                        :started_at nil
                                        :completed_at nil
                                        :last_error nil}))

(def SESSION_TITLE_TTL_SECONDS (* 60 60 24 7))
(def ^:private SESSION_TITLES_CACHE_MAX 512)

(defn session-title-key
  [session-id]
  (str "knoxx:session-title:" session-id))

(defn resolved
  [value]
  (js/Promise.resolve value))

(declare generate-session-title!)

(defn ^:async run-queued-session-title-task!
  [tail task-fn]
  (try
    (await tail)
    (catch :default _
      nil))
  (await (task-fn)))

(defn ^:async recover-session-title-tail!
  [task]
  (try
    (await task)
    (catch :default _
      nil)))

(defn- enqueue-session-title-generation!
  "Serialize Proxx-backed title generation so cache misses cannot fan out into
   a provider request storm. The returned promise preserves the task result;
   the queue tail always recovers so one failed naming request does not stall
   later titles."
  [task-fn]
  (let [task (run-queued-session-title-task! @session-title-generation-tail* task-fn)]
    (reset! session-title-generation-tail* (recover-session-title-tail! task))
    task))

(defn sanitize-session-title
  [value]
  (let [text (-> (str (or value ""))
                 (str/replace #"\s+" " ")
                 str/trim
                 (str/replace #"^[`'\"“”‘’]+|[`'\"“”‘’]+$" "")
                 str/trim)
        lowered (str/lower-case text)
        text (cond
               (str/starts-with? lowered "title: ") (subs text 7)
               (str/starts-with? lowered "title-") (subs text 6)
               (str/starts-with? lowered "title:") (subs text 6)
               :else text)
        text (str/trim text)]
    (when-not (str/blank? text)
      (subs text 0 (min 160 (count text))))))

(defn heuristic-session-title
  [seed-text]
  (let [words (->> (str/split-lines (or seed-text ""))
                   (map str/trim)
                   (remove str/blank?)
                   (take 2)
                   (map #(str/replace % #"^[#>*\-\d.\s]+" ""))
                   (map str/trim)
                   (remove str/blank?)
                   (str/join " "))
        cleaned (some-> words str/lower-case sanitize-session-title)]
    (or cleaned "Untitled session")))

(defn acceptable-session-title?
  [value]
  (let [title (sanitize-session-title value)
        lowered (some-> title str/lower-case)]
    (boolean
     (and title
          (>= (count title) 4)
          (not (contains? #{"title"
                            "session"
                            "chat"
                            "new chat"
                            "untitled"
                            "untitled session"
                            "res"}
                          lowered))))))

(defn normalize-session-title
  ([value] (normalize-session-title value nil))
  ([value fallback]
   (let [title (sanitize-session-title value)
         fallback-title (sanitize-session-title fallback)]
     (cond
       (acceptable-session-title? title) title
       (acceptable-session-title? fallback-title) fallback-title
       :else nil))))

(defn session-title-seed-text
  [rows]
  (let [user-texts (->> (or rows [])
                        (filter #(= "user" (:role %)))
                        (map #(str/trim (str (or (:text %) ""))))
                        (remove str/blank?)
                        vec)
        substantive (first (filter (fn [text]
                                     (or (>= (count text) 12)
                                         (>= (count (str/split text #"\s+")) 3)))
                                   user-texts))
        combined (some->> user-texts
                          (take 3)
                          (str/join " ")
                          str/trim
                          not-empty)
        fallback (->> (or rows [])
                      (map #(str/trim (str (or (:text %) ""))))
                      (remove str/blank?)
                      first)]
    (or substantive combined fallback "")))

(defn title-from-reasoning-content
  [value]
  (let [text (str (or value ""))]
    (or (some-> (re-find #"(?i)(?:i(?:'|’)ll|i will) go with\s+[\"“]([^\"”]{4,80})[\"”]" text)
                second
                sanitize-session-title)
        (some->> (re-seq #"[\"“]([^\"”]{4,80})[\"”]" text)
                 last
                 second
                 sanitize-session-title))))

(defn parse-json-object
  [value]
  (row-extra/parse-session-title-extra value))

(defn session-title-row-entry
  [row]
  (let [extra (or (row-extra/parse-session-title-extra (:extra row)) {})
        kind (or (:kind row)
                 (:event_kind row)
                 (get extra :kind)
                 (get extra :event_kind))
        raw-title (or (get extra :title)
                      (:title row)
                      (when (= kind "knoxx.session_title")
                        (:text row)))
        title (normalize-session-title raw-title)
        title-model (or (get extra :title_model)
                        (get extra :titleModel)
                        (:title_model row)
                        (:titleModel row)
                        (:model row))
        updated-at (or (:ts row)
                       (:created_at row)
                       (:updated_at row)
                       (time/now-iso))]
    (when (and (= kind "knoxx.session_title") title)
      {:title title
       :title_model title-model
       :updated_at updated-at})))

(defn stored-session-title-entry
  [session-id rows]
  (when-let [entry (some->> (or rows []) reverse (keep session-title-row-entry) first)]
    (assoc entry :session session-id)))

(defn- evict-stale-titles!
  "When session-titles* exceeds SESSION_TITLES_CACHE_MAX, evict oldest entries."
  []
  (swap! session-titles*
         (fn [titles]
           (if (<= (count titles) SESSION_TITLES_CACHE_MAX)
             titles
             (let [sorted (sort-by (fn [[_ entry]]
                                     (or (:updated_at entry) ""))
                                   titles)
                   drop-n (- (count sorted) SESSION_TITLES_CACHE_MAX)]
               (into {} (drop drop-n sorted))))))
  (swap! session-title-promises*
         (fn [promises]
           (if (<= (count promises) SESSION_TITLES_CACHE_MAX)
             promises
             (let [known (set (keys @session-titles*))]
               (select-keys promises known))))))

(defn ^:async persist-cached-session-title!
  [_redis-client session-id entry]
  (try
    (await (mongo-titles/upsert-title! session-id entry))
    (catch :default err
      (.warn js/console "Failed to persist session title cache into Mongo" err)
      nil)))

(defn ^:async clear-cached-session-title!
  [_redis-client session-id]
  (try
    (await (mongo-titles/delete-title! session-id))
    (catch :default err
      (.warn js/console "Failed to clear session title cache from Mongo" err)
      nil)))

(defn cache-session-title-entry!
  [session-id title title-model updated-at]
  (let [resolved {:title (or (normalize-session-title title) "Untitled session")
                  :title_model title-model
                  :session session-id
                  :updated_at (or updated-at (time/now-iso))}]
    (swap! session-titles* assoc session-id resolved)
    (swap! session-title-promises* dissoc session-id)
    (evict-stale-titles!)
    (persist-cached-session-title! nil session-id resolved)
    resolved))

(defn clear-session-title-entry!
  [session-id]
  (swap! session-titles* dissoc session-id)
  (swap! session-title-promises* dissoc session-id)
  (clear-cached-session-title! nil session-id)
  nil)

(defn ^:async get-cached-session-title!
  [session-id]
  (let [session-id (str (or session-id ""))]
    (cond
      (str/blank? session-id)
      nil

      (contains? @session-titles* session-id)
      (get @session-titles* session-id)

      :else
      (let [entry (await (mongo-titles/get-title! session-id))]
        (when entry
          (swap! session-titles* assoc session-id entry))
        entry))))

(defn session-title-event
  [config session-id title title-model]
  (let [event-id (str "knoxx:session-title:" session-id)
        ts (time/now-iso)
        normalized-title (or (normalize-session-title title) "Untitled session")]
    {:schema "openplanner.event.v1"
     :id event-id
     :ts ts
     :source "knoxx"
     :kind "knoxx.session_title"
     :source_ref {:project (:session-project-name config)
                  :session session-id
                  :message event-id}
     :text normalized-title
     :meta {:role "system"
            :author "knoxx"
            :model title-model
            :tags ["knoxx" "session_title" "metadata"]}
     :extra {:kind "knoxx.session_title"
             :title normalized-title
             :title_model title-model
             :session_id session-id}}))

(defn ^:async persist-session-title!
  [config session-id title title-model]
  (let [client (openplanner-client/client config)]
    (if (or (str/blank? (str session-id))
            (not (openplanner-client/enabled? client)))
      nil
      (try
        (await (openplanner-client/events! client [(session-title-event config session-id title title-model)]))
        (catch :default err
          (.warn js/console "Failed to persist session title into OpenPlanner" err)
          nil)))))

(defn cache-session-title!
  [_runtime _config session-id title title-model]
  (let [session-id (str (or session-id ""))
        resolved (cache-session-title-entry! session-id title title-model nil)]
    resolved))

(defn ^:async preload-session-title-entry!
  [config session-id]
  (let [client (openplanner-client/client config)]
    (try
      (let [body (await (openplanner-client/session! client
                                                     session-id
                                                     {:project (:session-project-name config)}))]
        (when-let [entry (stored-session-title-entry session-id (:rows body))]
          (cache-session-title-entry! session-id
                                      (:title entry)
                                      (:title_model entry)
                                      (:updated_at entry))))
      (catch :default _
        nil))))

(defn ^:async load-session-titles!
  [_runtime config]
  (let [client (openplanner-client/client config)]
    (if-not (openplanner-client/enabled? client)
      @session-titles*
      (try
        (let [body (await (openplanner-client/sessions! client {:project (:session-project-name config)}))
              session-ids (->> (or (:rows body) [])
                               (map :session)
                               (map str)
                               (remove str/blank?)
                               distinct
                               (take 64)
                               vec)]
          (when (seq session-ids)
            (await (js/Promise.all
                    (clj->js (mapv #(preload-session-title-entry! config %) session-ids)))))
          @session-titles*)
        (catch :default err
          (.warn js/console "Failed to preload session titles from OpenPlanner" err)
          @session-titles*)))))

(defn ^:async resolve-session-title!
  [config seed-text]
  (let [fallback (heuristic-session-title seed-text)]
    (try
      (let [entry (await (enqueue-session-title-generation! #(generate-session-title! config seed-text)))]
        {:title (or (normalize-session-title (:title entry) fallback)
                    fallback)
         :title_model (:title_model entry)})
      (catch :default _
        {:title fallback
         :title_model nil}))))

(defn- session-title-model
  [config]
  (or (some-> (:session-title-model config) str str/trim not-empty)
      (some-> (:proxx-default-model config) str str/trim not-empty)
      "glm-5"))

(defn- session-title-request
  [config seed-text]
  {:model (session-title-model config)
   :messages [{:role "system"
               :content "You create very short, useful session titles. Return only the title text, 2 to 6 words, with no quotes, no markdown, and no explanation."}
              {:role "user"
               :content (str "Create a concise title for this Knoxx session based on the opening request.\n\nRequest:\n"
                             (or (text/value->preview-text seed-text 900) ""))}]
   :temperature 0.1
   :max_tokens 24
   :stream false})

(defn- completion->session-title
  [{:keys [ok? model content reasoning-content]} fallback fallback-model]
  (if ok?
    (let [title-candidate (or (normalize-session-title content)
                              (title-from-reasoning-content reasoning-content)
                              fallback)]
      {:title (or (normalize-session-title title-candidate fallback) fallback)
       :title_model (or model fallback-model)})
    {:title fallback
     :title_model nil}))

(defn ^:async generate-session-title!
  [config seed-text]
  (let [fallback (heuristic-session-title seed-text)]
    (if (or (str/blank? seed-text)
            (str/blank? (:proxx-base-url config))
            (str/blank? (:proxx-auth-token config)))
      {:title fallback
       :title_model nil}
      (try
        (completion->session-title
         (await (proxx/chat-completion! config (session-title-request config seed-text)))
         fallback
         (session-title-model config))
        (catch :default _
          {:title fallback
           :title_model nil})))))

(defn ^:async resolve-session-title-from-rows!
  [config session-id rows]
  (if-let [stored (stored-session-title-entry session-id rows)]
    (assoc stored :stored true)
    (let [seed-text (session-title-seed-text (vec (or rows [])))
          entry (await (resolve-session-title! config seed-text))]
      {:title (:title entry)
       :title_model (:title_model entry)
       :session session-id
       :updated_at (time/now-iso)
       :stored false})))

(defn- cache-resolved-session-title!
  [runtime config session-id entry]
  (if (:stored entry)
    (cache-session-title-entry! session-id (:title entry) (:title_model entry) (:updated_at entry))
    (cache-session-title! runtime config session-id (:title entry) (:title_model entry))))

(defn ^:async compute-session-title!
  [runtime config session-id rows fetch-session-rows!]
  (if-let [cached (await (get-cached-session-title! session-id))]
    cached
    (try
      (let [resolved-rows (if (seq rows)
                            rows
                            (await (fetch-session-rows! config session-id)))
            entry (await (resolve-session-title-from-rows! config session-id resolved-rows))]
        (cache-resolved-session-title! runtime config session-id entry))
      (catch :default _
        (cache-session-title! runtime config session-id "Untitled session" nil)))))

(defn ensure-session-title!
  ([runtime config session-id rows force? fetch-session-rows!]
   (let [session-id (str (or session-id ""))]
     (when force?
       (clear-session-title-entry! session-id))
     (cond
       (str/blank? session-id)
       (resolved {:title "Untitled session"
                  :title_model nil})

       (contains? @session-title-promises* session-id)
       (get @session-title-promises* session-id)

       :else
       (let [title-promise (compute-session-title! runtime config session-id rows fetch-session-rows!)]
         (swap! session-title-promises* assoc session-id title-promise)
         title-promise)))))

(defn ^:async prime-session-title!
  [runtime config session-id seed-text]
  (if-let [cached (await (get-cached-session-title! session-id))]
    cached
    (try
      (let [entry (await (resolve-session-title! config seed-text))]
        (cache-session-title! runtime config session-id (:title entry) (:title_model entry)))
      (catch :default _
        (cache-session-title! runtime config session-id (heuristic-session-title seed-text) nil)))))

(defn maybe-prime-session-title!
  [runtime config session-id seed-text]
  (let [session-id (str (or session-id ""))
        seed-text (str (or seed-text ""))]
    (when (and (not (str/blank? session-id))
               (not (str/blank? seed-text))
               (not (contains? @session-title-promises* session-id)))
      (let [title-promise (prime-session-title! runtime config session-id seed-text)]
        (swap! session-title-promises* assoc session-id title-promise)
        title-promise))))

(defn- session-ids-from-response
  [body limit]
  (cond->> (->> (or (:rows body) [])
                (map :session) (map str) (remove str/blank?) distinct)
    limit (take limit)))

(defn- init-backfill-state!
  [session-ids force]
  (reset! session-title-backfill* {:active true, :processed 0, :total (count session-ids),
                                   :failed 0, :force (boolean force),
                                   :started_at (time/now-iso), :completed_at nil, :last_error nil}))

(defn- complete-backfill!
  []
  (swap! session-title-backfill* assoc :active false :completed_at (time/now-iso))
  @session-title-backfill*)

(defn- record-backfill-error!
  [err]
  (swap! session-title-backfill*
         (fn [state] (-> state (update :processed (fnil inc 0)) (update :failed (fnil inc 0)) (assoc :last_error (str err))))))

(defn ^:async backfill-one-session!
  [runtime config fetch-session-rows! force session-id]
  (when force (clear-session-title-entry! session-id))
  (try
    (let [title-rows (await (fetch-session-rows! config session-id))
          entry (await (resolve-session-title-from-rows! config session-id title-rows))]
      (cache-resolved-session-title! runtime config session-id entry))
    (catch :default _
      (cache-session-title! runtime config session-id "Untitled session" nil)))
  (swap! session-title-backfill* update :processed (fnil inc 0)))

(defn- fail-backfill!
  [err]
  (swap! session-title-backfill* assoc
         :active false
         :completed_at (time/now-iso)
         :last_error (str err))
  nil)

(defn ^:async run-session-title-backfill!
  [runtime config session-ids force fetch-session-rows!]
  (try
    (doseq [session-id session-ids]
      (try
        (await (backfill-one-session! runtime config fetch-session-rows! force session-id))
        (catch :default err
          (record-backfill-error! err))))
    (complete-backfill!)
    (catch :default err
      (fail-backfill! err))))

(defn ^:async start-session-title-backfill!
  [runtime config {:keys [force limit]} fetch-session-rows!]
  (if (:active @session-title-backfill*)
    @session-title-backfill*
    (let [client (openplanner-client/client config)]
      (try
        (let [body (await (openplanner-client/sessions! client {:project (:session-project-name config)}))
              session-ids (vec (session-ids-from-response body limit))]
          (init-backfill-state! session-ids force)
          (if (empty? session-ids)
            (complete-backfill!)
            (do
              (run-session-title-backfill! runtime config session-ids force fetch-session-rows!)
              @session-title-backfill*)))
        (catch :default err
          (fail-backfill! err)
          @session-title-backfill*)))))
