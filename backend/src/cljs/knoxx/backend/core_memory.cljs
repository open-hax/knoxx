(ns knoxx.backend.core-memory
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [system-admin? ctx-org-id ctx-membership-id ctx-user-id ctx-permitted?]]
            [knoxx.backend.document-state :refer [normalize-relative-path]]
            [knoxx.backend.http :as backend-http :refer [js-array-seq]]
            [knoxx.backend.runtime-config :refer [cfg]]))

(defn parse-json-object
  [value]
  (cond
    (map? value) value
    (string? value) (try
                      (js->clj (.parse js/JSON value) :keywordize-keys true)
                      (catch :default _ nil))
    :else nil))

(defn row-extra-map
  [row]
  (or (parse-json-object (:extra row)) {}))

(def devel-path-pattern
  #"((?:orgs|packages|services|docs|spec|specs|tools|ecosystems|src|worktrees|\.pi)/[A-Za-z0-9._~:/+-]+)")

(def url-pattern
  #"https?://[A-Za-z0-9._~:/?#\[\]@!$&'()*+,;=%-]+")

(defn trim-mention-token
  [value]
  (-> (str value)
      (str/replace #"^[\s`'\"\(\[\{<]+" "")
      (str/replace #"[\s`'\"\)\]\}>:;,.!?]+$" "")))

(defn normalize-web-url
  [value]
  (let [raw (trim-mention-token value)]
    (if (str/blank? raw)
      nil
      (try
        (let [parsed (js/URL. raw)]
          (set! (.-hash parsed) "")
          (when (str/blank? (.-pathname parsed))
            (set! (.-pathname parsed) "/"))
          (.toString parsed))
        (catch :default _ nil)))))

(defn normalize-devel-path
  [value]
  (let [trimmed (trim-mention-token value)
        no-prefix (cond
                    (str/starts-with? trimmed "/app/workspace/devel/") (subs trimmed (count "/app/workspace/devel/"))
                    (str/starts-with? trimmed (:workspace-root (cfg))) (subs trimmed (inc (count (:workspace-root (cfg)))))
                    :else trimmed)
        normalized (normalize-relative-path no-prefix)]
    (when (and (not (str/blank? normalized))
               (re-find #"^(orgs|packages|services|docs|spec|specs|tools|ecosystems|src|worktrees|\.pi)/" normalized))
      normalized)))

(defn extract-mentioned-urls
  [text]
  (->> (re-seq url-pattern (or text ""))
       (map normalize-web-url)
       (remove nil?)
       distinct
       vec))

(defn extract-mentioned-devel-paths
  [text]
  (->> (re-seq devel-path-pattern (or text ""))
       (map second)
       (map normalize-devel-path)
       (remove nil?)
       distinct
       vec))

(defn session-visible?
  [ctx rows]
  (cond
    (nil? ctx) true
    (system-admin? ctx) true
    :else
    (let [extras (map row-extra-map rows)
          org-ids (into #{} (keep #(some-> % :org_id str not-empty)) extras)
          membership-ids (into #{} (keep #(some-> % :membership_id str not-empty)) extras)
          user-ids (into #{} (keep #(some-> % :user_id str not-empty)) extras)
          same-org? (contains? org-ids (str (ctx-org-id ctx)))]
      (cond
        (empty? org-ids) false
        (not same-org?) false
        (ctx-permitted? ctx "agent.memory.cross_session") true
        :else (or (contains? membership-ids (str (ctx-membership-id ctx)))
                  (contains? user-ids (str (ctx-user-id ctx))))))))

(defn fetch-openplanner-session-rows!
  [config session-id]
  (-> (backend-http/openplanner-request! config "GET" (str "/v1/sessions/" (js/encodeURIComponent (str session-id))
                                               "?project=" (js/encodeURIComponent (:session-project-name config))))
      (.then (fn [body]
               (vec (or (:rows body) []))))))

(defn authorized-session-ids!
  [config ctx session-ids]
  (let [session-ids (->> session-ids
                         (map str)
                         (remove str/blank?)
                         distinct
                         vec)]
    (if (or (nil? ctx) (system-admin? ctx) (empty? session-ids))
      (js/Promise.resolve (set session-ids))
      (.then (js/Promise.all
              (clj->js
               (map (fn [session-id]
                      (.then (fetch-openplanner-session-rows! config session-id)
                             (fn [rows]
                               {:session session-id
                                :allowed (session-visible? ctx rows)})
                             (fn [_]
                               {:session session-id
                                :allowed false})))
                    session-ids)))
             (fn [results]
               (->> (js-array-seq results)
                    (filter :allowed)
                    (map :session)
                    set))))))

(defn hit-session-id
  [hit]
  (or (:session hit)
      (get-in hit [:metadata :session])
      (get-in hit [:extra :session])))

(defn filter-authorized-memory-hits!
  [config ctx hits]
  (let [hits (vec hits)
        session-ids (map hit-session-id hits)]
    (-> (authorized-session-ids! config ctx session-ids)
        (.then (fn [allowed]
                 (->> hits
                      (filter (fn [hit]
                                (contains? allowed (str (or (hit-session-id hit) "")))))
                      vec))))))

