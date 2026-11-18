;; Local subset informed by https://docs.optimizely.com/cms-saas/api ; not vendor parity.
(ns big-pi.optimizely.domain.cms
  (:require [big-pi.optimizely.law.cms :as law]
            [clojure.string :as str]))

(def capabilities
  "Declared local routes, extensions and unsupported capabilities."
  {:profile "cms-saas-rest-v1-local-subset"
   :routes ["/v1/content" "/v1/content/{key}" "/v1/content/{key}/versions"
            "/v1/content/{key}/versions/{version}" "/v1/content/{key}/locales"
            "/v1/contenttypes" "/v1/contenttypes/{key}" "/v1/locales" "/v1/locales/{key}"]
   :local-extensions ["GET /v1/content"]
   :unsupported ["media" "bindings" "graphql" "publication"]})

(defn initial-state
  "Seed a new immutable local CMS state." []
  {:generation 0 :next-key 1 :content {} :history []
   :contenttypes {"Article" {:key "Article" :displayName "Article" :properties {}}}
   :locales (into {} (map (fn [k] [k {:key k :displayName k}]) ["en" "de" "es"]))})

(defn- response [state status body & [headers]]
  {:state state :response {:status status :headers (or headers {}) :body body}})

(defn- problem [state status code detail]
  (response state status {:type "about:blank" :title detail :status status :code code :detail detail}
            {"Content-Type" "application/problem+json"}))

(defn- etag [generation] (str "\"" generation "\""))
(defn- current-tag [entity] (etag (or (:generation entity) 0)))
(defn- visible [entity]
  (cond-> (dissoc entity :generation :versions :next-version)
    (contains? entity :versions)
    (assoc :locales (->> (:versions entity) vals (map :locale) distinct sort vec))))
(defn- prefer? [request] (str/includes? (get-in request [:headers "prefer"] "") "return=representation"))
(defn- merge-patch [target patch]
  (if (map? patch)
    (reduce-kv (fn [m k v] (if (nil? v) (dissoc m k) (assoc m k (merge-patch (get m k) v))))
               (if (map? target) target {}) patch)
    patch))
(defn- changed [state path value]
  (let [generation (inc (:generation state))]
    (-> state
        (update :history conj {:generation (:generation state) :path path :value (get-in state path)})
        (assoc :generation generation)
        (assoc-in path (assoc value :generation generation)))))
(defn- read-one [state request entity]
  (let [tag (current-tag entity)]
    (if (#{tag "*"} (get-in request [:headers "if-none-match"]))
      (response state 304 nil {"ETag" tag})
      (response state 200 (visible entity) {"ETag" tag}))))
(defn- write-result [state request entity status location]
  (let [representation? (prefer? request)]
    (response state (if (and (= status 204) representation?) 200 status)
              (when representation? (visible entity))
              (cond-> {"ETag" (current-tag entity)} location (assoc "Location" location)))))
(defn- stale? [request entity]
  (let [match (get-in request [:headers "if-match"])]
    (and match (not= match "*") (not= match (current-tag entity)))))
(defn- page [state request values]
  (if-let [{:keys [pageIndex pageSize]} (law/paging (:query request))]
    (let [locales (get-in request [:query "locales"])
          statuses (get-in request [:query "statuses"])
          filtered (filter #(and (or (nil? locales) (some #{(:locale %)} locales))
                                 (or (nil? statuses) (some #{(:status %)} statuses))) values)
          items (->> filtered (drop (* pageIndex pageSize)) (take pageSize) (mapv visible))]
      (response state 200 {:items items :pageIndex pageIndex :pageSize pageSize :totalCount (count filtered)}))
    (problem state 400 "invalid_query" "Invalid paging or filters")))

(defn- create-version [state request key data]
  (let [content (get-in state [:content key])]
    (cond
      (nil? content) (problem state 404 "not_found" "Content not found")
      (and (map? data) (contains? data :status) (not= "draft" (:status data))) (problem state 403 "publication_forbidden" "Status cannot be set by draft writes")
      (not (law/version? data false)) (problem state 400 "invalid_body" "Invalid content version")
      (not (contains? (:locales state) (:locale data))) (problem state 400 "invalid_locale" "Unknown locale")
      :else (let [id (str (:next-version content))
                  path [:content key :versions id]
                  version (assoc data :key key :version id :status "draft")
                  updated (-> state (changed path version) (update-in [:content key :next-version] inc)
                              (assoc-in [:content key :generation] (inc (:generation state))))]
              (write-result updated request (get-in updated path) 201 (str "/v1/content/" key "/versions/" id))))))

(defn- next-free-content-number [state]
  (loop [number (:next-key state)]
    (if (contains? (:content state) (str "content-" number))
      (recur (inc number))
      number)))

(defn- create-content [state request]
  (let [data (:body request)
        generated-number (when-not (:key data) (next-free-content-number state))
        key (or (:key data) (str "content-" generated-number))]
    (cond
      (and (map? (:initialVersion data)) (contains? (:initialVersion data) :status) (not= "draft" (get-in data [:initialVersion :status]))) (problem state 403 "publication_forbidden" "Status cannot be set by draft writes")
      (contains? data :container) (problem state 501 "unsupported_operation" "Content containers are not implemented")
      (not (law/content? data)) (problem state 400 "invalid_body" "Invalid content")
      (not (contains? (:contenttypes state) (:contentType data))) (problem state 400 "invalid_content_type" "Unknown content type")
      (not (contains? (:locales state) (get-in data [:initialVersion :locale]))) (problem state 400 "invalid_locale" "Unknown locale")
      (contains? (:content state) key) (problem state 409 "already_exists" "Content already exists")
      :else (let [content (merge (select-keys data [:contentType :container]) {:key key :versions {} :next-version 1})
                  s (-> state (changed [:content key] content)
                        (assoc :next-key (if generated-number (inc generated-number) (inc (:next-key state)))))
                  result (create-version s (assoc request :headers (assoc (:headers request) "prefer" "return=representation")) key (:initialVersion data))
                  new-state (:state result)]
              (write-result new-state request (get-in new-state [:content key :versions "1"]) 201 (str "/v1/content/" key))))))

(defn- patch-entity [state request path validator]
  (let [entity (get-in state path) data (:body request) ct (get-in request [:headers "content-type"])]
    (cond
      (nil? entity) (problem state 404 "not_found" "Resource not found")
      (stale? request entity) (problem state 412 "precondition_failed" "ETag does not match")
      (and ct (not (str/starts-with? ct "application/merge-patch+json"))) (problem state 415 "unsupported_media_type" "Expected JSON Merge Patch")
      (and (map? data) (contains? data :status) (not= "draft" (:status data))) (problem state 403 "publication_forbidden" "Status cannot be set by draft writes")
      (and (= :content (first path)) (= 2 (count path)) (contains? data :container))
      (problem state 501 "unsupported_operation" "Content containers are not implemented")
      (not (validator data)) (problem state 400 "invalid_body" "Invalid patch")
      (and (= :versions (nth path 2 nil)) (contains? data :locale) (not= (:locale entity) (:locale data)))
      (problem state 400 "invalid_body" "A version cannot change locale")
      (and (#{:contenttypes :locales} (first path))
           (not (law/entity? (select-keys (merge-patch entity data)
                                          (if (= :locales (first path)) law/locale-fields law/type-fields))
                             (if (= :locales (first path)) law/locale-fields law/type-fields) false)))
      (problem state 400 "invalid_body" "Patch must preserve required fields")
      (and (= :versions (nth path 2 nil))
           (let [v (merge-patch entity data)]
             (or (not (law/version? (select-keys v law/version-fields) false))
                 (not (contains? (:locales state) (:locale v))))))
      (problem state 400 "invalid_body" "Patch must preserve required version fields and a known locale")
      :else (let [updated (changed state path (merge-patch entity data))
                  updated (if (= :versions (nth path 2 nil))
                            (assoc-in updated [:content (second path) :generation] (:generation updated))
                            updated)]
              (write-result updated request (get-in updated path) 204 nil)))))

(defn- entity-collection [state request kind fields]
  (case (:method request)
    :get (page state request (sort-by :key (vals (get state kind))))
    :post (let [data (:body request) key (:key data) path [kind key]]
            (cond
              (not (law/entity? data fields false)) (problem state 400 "invalid_body" "Invalid resource")
              (get-in state path) (problem state 409 "already_exists" "Resource already exists")
              :else (let [s (changed state path data)]
                      (write-result s request (get-in s path) 201 (str "/v1/" (name kind) "/" key)))))
    (problem state 501 "unsupported_operation" "Operation is not implemented")))

(defn- entity-item [state request kind key fields]
  (let [path [kind key] entity (get-in state path)]
    (case (:method request)
      :get (if entity (read-one state request entity) (problem state 404 "not_found" "Resource not found"))
      :patch (patch-entity state request path #(and (law/entity? % (disj fields :key) true)))
      (problem state 501 "unsupported_operation" "Operation is not implemented"))))

(defn- content-route [state request parts]
  (let [[_ key section version] parts
        method (:method request)
        entity (get-in state [:content key])]
    (cond
      (= 1 (count parts))
      (case method
        :get (page state request (sort-by :key (vals (:content state))))
        :post (create-content state request)
        (problem state 501 "unsupported_operation" "Operation is not implemented"))

      (= 2 (count parts))
      (case method
        :get (if entity (read-one state request entity) (problem state 404 "not_found" "Content not found"))
        :patch (patch-entity state request [:content key]
                             #(and (law/valid-fields? % #{:container})
                                   (or (nil? (:container %)) (string? (:container %)))))
        (problem state 501 "unsupported_operation" "Operation is not implemented"))

      (and (= 3 (count parts)) (= section "versions"))
      (cond
        (nil? entity) (problem state 404 "not_found" "Content not found")
        (= method :get) (page state request (sort-by #(parse-long (:version %)) (vals (:versions entity))))
        (= method :post) (create-version state request key (:body request))
        :else (problem state 501 "unsupported_operation" "Operation is not implemented"))

      (and (= 4 (count parts)) (= section "versions"))
      (let [path [:content key :versions version] v (get-in state path)]
        (case method
          :get (if v (read-one state request v) (problem state 404 "not_found" "Version not found"))
          :patch (patch-entity state request path #(law/version? % true))
          (problem state 501 "unsupported_operation" "Operation is not implemented")))

      (and (= 3 (count parts)) (= section "locales") (= method :get))
      (if entity
        (response state 200 {:items (->> (:versions entity) vals (map :locale) distinct sort vec)})
        (problem state 404 "not_found" "Content not found"))
      :else (problem state 501 "unsupported_operation" "Operation is not implemented"))))

(defn- dispatch [state request]
  (let [path (:path request)
        parts (str/split path #"/" -1)
        segments (vec (drop 2 parts))]
    (cond
      (or (not (str/starts-with? path "/v1/"))
          (some #(or (empty? %) (= % ".") (= % "..") (str/includes? % "%") (str/includes? % "\\")) segments))
      (problem state 400 "invalid_path" "Invalid path")
      (= (first segments) "content")
      (if (every? law/safe-key? (filter some? [(second segments)]))
        (content-route state request segments)
        (problem state 400 "invalid_path" "Invalid content key"))
      (#{"contenttypes" "locales"} (first segments))
      (let [kind (keyword (first segments)) fields (if (= kind :locales) law/locale-fields law/type-fields)]
        (case (count segments)
          1 (entity-collection state request kind fields)
          2 (if (law/safe-key? (second segments))
              (entity-item state request kind (second segments) fields)
              (problem state 400 "invalid_path" "Invalid resource key"))
          (problem state 501 "unsupported_operation" "Operation is not implemented")))
      :else (problem state 501 "unsupported_operation" "Operation is not implemented"))))

(defn handle
  "Apply one decoded local request, returning state and response without effects." [state request]
  (try
    (if (law/request? request)
      (dispatch state request)
      (problem state 400 "invalid_request" "Invalid request contract"))
    (catch :default _
      (problem state 400 "invalid_request" "Request could not be processed"))))
