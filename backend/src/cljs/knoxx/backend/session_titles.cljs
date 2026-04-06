(ns knoxx.backend.session-titles
  (:require [clojure.string :as str]
            [knoxx.backend.http :as backend-http]
            [knoxx.backend.runtime-config :as runtime-config]
            [knoxx.backend.text :as text]))

(defonce session-titles* (atom {}))
(defonce session-title-promises* (atom {}))
(defonce session-title-backfill* (atom {:active false
                                        :processed 0
                                        :total 0
                                        :failed 0
                                        :force false
                                        :started_at nil
                                        :completed_at nil
                                        :last_error nil}))

(declare generate-session-title!)

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
      (subs text 0 (min 80 (count text))))))

(defn heuristic-session-title
  [seed-text]
  (let [line (->> (str/split-lines (or seed-text ""))
                  (map str/trim)
                  (remove str/blank?)
                  first)
        cleaned (some-> line
                        (str/replace #"^[#>*\-\d.\s]+" "")
                        sanitize-session-title)]
    (or cleaned "Untitled session")))

(defn session-title-cache-path
  [runtime config]
  (.join (aget runtime "path") (:workspace-root config) ".knoxx" "session-titles.json"))

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
       (not (str/blank? fallback-title)) fallback-title
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
                          (str/join "\n\n")
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

(defn persist-session-titles!
  [runtime config]
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        cache-path (session-title-cache-path runtime config)
        parent (.dirname node-path cache-path)
        payload (.stringify js/JSON (clj->js @session-titles*) nil 2)]
    (-> (.mkdir node-fs parent #js {:recursive true})
        (.then (fn []
                 (.writeFile node-fs cache-path payload "utf8"))))))

(defn cache-session-title!
  [runtime config session-id title title-model]
  (let [resolved {:title (or (normalize-session-title title) "Untitled session")
                  :title_model title-model
                  :session session-id
                  :updated_at (runtime-config/now-iso)}]
    (swap! session-titles* assoc session-id resolved)
    (swap! session-title-promises* dissoc session-id)
    (-> (persist-session-titles! runtime config)
        (.catch (fn [err]
                  (.error js/console "Failed to persist session title cache" err)
                  nil)))
    resolved))

(defn load-session-titles!
  [runtime config]
  (let [node-fs (aget runtime "fs")
        cache-path (session-title-cache-path runtime config)]
    (-> (.readFile node-fs cache-path "utf8")
        (.then (fn [raw]
                 (let [parsed (js->clj (.parse js/JSON raw))
                       loaded (reduce-kv (fn [acc session-id entry]
                                           (let [session-id (str session-id)
                                                 title (normalize-session-title (get entry "title"))
                                                 title-model (or (get entry "title_model")
                                                                 (get entry "titleModel"))
                                                 updated-at (or (get entry "updated_at")
                                                                (get entry "updatedAt")
                                                                (runtime-config/now-iso))]
                                             (if title
                                               (assoc acc session-id {:title title
                                                                      :title_model title-model
                                                                      :session session-id
                                                                      :updated_at updated-at})
                                               acc)))
                                         {}
                                         parsed)]
                   (reset! session-titles* loaded)
                   loaded)))
        (.catch (fn [_]
                  (js/Promise.resolve @session-titles*))))))

(defn resolve-session-title!
  [config seed-text]
  (let [fallback (heuristic-session-title seed-text)]
    (-> (generate-session-title! config seed-text)
        (.then (fn [entry]
                 {:title (or (normalize-session-title (:title entry) fallback)
                             fallback)
                  :title_model (:title_model entry)}))
        (.catch (fn [_]
                  (js/Promise.resolve {:title fallback
                                       :title_model nil}))))))

(defn generate-session-title!
  [config seed-text]
  (let [fallback (heuristic-session-title seed-text)]
    (if (or (str/blank? seed-text)
            (str/blank? (:proxx-base-url config))
            (str/blank? (:proxx-auth-token config)))
      (js/Promise.resolve {:title fallback
                           :title_model nil})
      (let [payload #js {:model "auto:cheapest"
                         :messages (clj->js [{:role "system"
                                              :content "You create very short, useful session titles. Return only the title text, 2 to 6 words, with no quotes, no markdown, and no explanation."}
                                             {:role "user"
                                              :content (str "Create a concise title for this Knoxx session based on the opening request.\n\nRequest:\n"
                                                            (or (text/value->preview-text seed-text 900) ""))}])
                         :temperature 0.1
                         :max_tokens 24
                         :stream false}]
        (-> (backend-http/fetch-json (str (:proxx-base-url config) "/v1/chat/completions")
                                     #js {:method "POST"
                                          :headers (backend-http/bearer-headers (:proxx-auth-token config))
                                          :body (.stringify js/JSON payload)})
            (.then (fn [resp]
                     (if (aget resp "ok")
                       (let [body (aget resp "body")
                             choices (or (aget body "choices") #js [])
                             first-choice (aget choices 0)
                             message (or (aget first-choice "message") #js {})
                             content (or (aget message "content")
                                         (aget first-choice "text")
                                         "")
                             reasoning-content (or (aget message "reasoning_content")
                                                   (aget message "reasoningContent")
                                                   "")
                             title-candidate (or (normalize-session-title content)
                                                 (title-from-reasoning-content reasoning-content)
                                                 fallback)]
                         {:title (or (normalize-session-title title-candidate fallback) fallback)
                          :title_model (or (aget body "model") "auto:cheapest")})
                       {:title fallback
                        :title_model nil})))
            (.catch (fn [_]
                      (js/Promise.resolve {:title fallback
                                           :title_model nil}))))))))

(defn ensure-session-title!
  ([runtime config session-id rows force? fetch-session-rows!]
   (let [session-id (str (or session-id ""))]
     (when force?
       (swap! session-titles* dissoc session-id))
     (cond
       (str/blank? session-id)
       (js/Promise.resolve {:title "Untitled session"
                            :title_model nil})

       (contains? @session-titles* session-id)
       (js/Promise.resolve (get @session-titles* session-id))

       (contains? @session-title-promises* session-id)
       (get @session-title-promises* session-id)

       :else
       (let [title-promise (-> (if (seq rows)
                                 (js/Promise.resolve rows)
                                 (fetch-session-rows! config session-id))
                               (.then (fn [resolved-rows]
                                        (resolve-session-title! config (session-title-seed-text (vec (or resolved-rows []))))))
                               (.then (fn [entry]
                                        (cache-session-title! runtime config session-id (:title entry) (:title_model entry))))
                               (.catch (fn [_]
                                         (cache-session-title! runtime config session-id "Untitled session" nil))))]
         (swap! session-title-promises* assoc session-id title-promise)
         title-promise)))))

(defn maybe-prime-session-title!
  [runtime config session-id seed-text]
  (let [session-id (str (or session-id ""))
        seed-text (str (or seed-text ""))]
    (when (and (not (str/blank? session-id))
               (not (str/blank? seed-text))
               (not (contains? @session-titles* session-id))
               (not (contains? @session-title-promises* session-id)))
      (let [title-promise (-> (resolve-session-title! config seed-text)
                              (.then (fn [entry]
                                       (cache-session-title! runtime config session-id (:title entry) (:title_model entry))))
                              (.catch (fn [_]
                                        (cache-session-title! runtime config session-id (heuristic-session-title seed-text) nil))))]
        (swap! session-title-promises* assoc session-id title-promise)
        title-promise))))

(defn start-session-title-backfill!
  [runtime config {:keys [force limit]} fetch-session-rows!]
  (if (:active @session-title-backfill*)
    (js/Promise.resolve @session-title-backfill*)
    (-> (backend-http/openplanner-request! config "GET"
                                          (str "/v1/sessions?project="
                                               (js/encodeURIComponent (:session-project-name config))))
        (.then (fn [body]
                 (let [session-ids (cond->> (->> (or (:rows body) [])
                                                 (map :session)
                                                 (map str)
                                                 (remove str/blank?)
                                                 distinct)
                                     limit (take limit))
                       session-ids (vec session-ids)]
                   (reset! session-title-backfill* {:active true
                                                    :processed 0
                                                    :total (count session-ids)
                                                    :failed 0
                                                    :force (boolean force)
                                                    :started_at (runtime-config/now-iso)
                                                    :completed_at nil
                                                    :last_error nil})
                   (if (empty? session-ids)
                     (do
                       (swap! session-title-backfill* assoc :active false :completed_at (runtime-config/now-iso))
                       @session-title-backfill*)
                     (letfn [(step [remaining]
                               (if-let [session-id (first remaining)]
                                 (do
                                   (when force
                                     (swap! session-titles* dissoc session-id))
                                   (-> (fetch-session-rows! config session-id)
                                       (.then (fn [title-rows]
                                                (let [seed-text (session-title-seed-text title-rows)
                                                      fallback-title (heuristic-session-title seed-text)]
                                                  (-> (resolve-session-title! config seed-text)
                                                      (.then (fn [entry]
                                                               (cache-session-title! runtime config session-id
                                                                                     (or (normalize-session-title (:title entry) fallback-title)
                                                                                         fallback-title)
                                                                                     (:title_model entry))))
                                                      (.catch (fn [_]
                                                                (cache-session-title! runtime config session-id fallback-title nil)))))))
                                       (.catch (fn [_]
                                                 (cache-session-title! runtime config session-id "Untitled session" nil)))
                                       (.then (fn [_]
                                                (swap! session-title-backfill* update :processed (fnil inc 0))))
                                       (.catch (fn [err]
                                                 (swap! session-title-backfill* (fn [state]
                                                                                  (-> state
                                                                                      (update :processed (fnil inc 0))
                                                                                      (update :failed (fnil inc 0))
                                                                                      (assoc :last_error (str err)))))
                                                 nil))
                                       (.then (fn [_]
                                                (step (rest remaining))))))
                                 (do
                                   (swap! session-title-backfill* assoc :active false :completed_at (runtime-config/now-iso))
                                   (js/Promise.resolve @session-title-backfill*))))]
                       (-> (step session-ids)
                           (.catch (fn [err]
                                     (swap! session-title-backfill* assoc
                                            :active false
                                            :completed_at (runtime-config/now-iso)
                                            :last_error (str err))
                                     nil)))
                       @session-title-backfill*)))))
        (.catch (fn [err]
                  (swap! session-title-backfill* assoc
                         :active false
                         :completed_at (runtime-config/now-iso)
                         :last_error (str err))
                  (js/Promise.resolve @session-title-backfill*))))))
