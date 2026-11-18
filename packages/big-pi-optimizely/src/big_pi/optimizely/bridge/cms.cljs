(ns big-pi.optimizely.bridge.cms
  "Exact-source import and draft-only return through injected HTTP transports."
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [promesa.core :as p]))

(defn- fail! [code message]
  (throw (ex-info message {:code code
                           :status (case code
                                     (:stale-or-conflicted :stale-source :field-mismatch :source-identity-mismatch) 409
                                     :outcome-uncertain 502
                                     :upstream-failure 502
                                     :source-identity-unavailable 502
                                     400)})))

(defn- fields! [request allowed]
  (when-not (and (map? request) (every? allowed (keys request)))
    (fail! :invalid-request "Unknown request fields")))

(defn- segment! [label value]
  ;; Only unreserved segments are accepted; no path encoding or ambiguous dot segments.
  (when-not (and (string? value) (<= 1 (count value) 100)
                 (re-matches #"[A-Za-z0-9_-]+" value))
    (fail! :invalid-request (str "Invalid " label)))
  value)

(defn- text! [label value maximum]
  (when-not (and (string? value) (<= 1 (count (str/trim value)) maximum))
    (fail! :invalid-payload (str "Invalid " label)))
  value)

(defn- locale! [value]
  (when-not (and (string? value) (<= (count value) 35)
                 (re-matches #"[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*" value))
    (fail! :invalid-payload "Invalid locale"))
  value)

(defn- response! [response operation]
  (when-not (and (map? response) (integer? (:status response))
                 (<= 200 (:status response) 299) (map? (:body response)))
    (throw (ex-info (str operation " failed")
                    {:code :upstream-failure
                     :status (if (and (integer? (:status response))
                                      (<= 400 (:status response) 599))
                               (:status response) 502)})))
  response)

(defn- vendor-path [key version]
  (str "/v1/content/" (segment! "content key" key) "/versions/" (segment! "version" version)))

(defn- property [payload field]
  (let [properties (:properties payload)
        field-key (keyword (segment! "field" field))]
    (when-not (and (map? properties) (contains? properties field-key)
                   (string? (get properties field-key))
                   (<= (count (get properties field-key)) 262144))
      (fail! :unsupported-property "Selected property must be a bounded plain string"))
    (get properties field-key)))

(defn- checked-source! [response key version field]
  (let [{:keys [body headers]} (response! response "Source revision read")
        etag (get headers "etag")
        source-id (get headers "x-big-pi-source-id")]
    (when-not (and (= key (:key body)) (= version (:version body)))
      (fail! :source-mismatch "Source response does not match requested revision"))
    (text! "ETag" etag 1024)
    (when-not (and (string? source-id) (<= 1 (count source-id) 100)
                   (re-matches #"[A-Za-z0-9_-]+" source-id))
      (fail! :source-identity-unavailable "Trusted source identity is missing or invalid"))
    (text! "title" (:displayName body) 200)
    (locale! (:locale body))
    (property body field)
    {:payload body :etag etag :source-id source-id}))

(defn- decode-payload! [format encoded]
  (when-not (and (= "big-pi-edn-v1" format) (string? encoded)
                 (<= 1 (count encoded) 65536))
    (fail! :invalid-provenance "Unsupported or missing source payload envelope"))
  (let [payload (try (reader/read-string encoded)
                     (catch :default _ (fail! :invalid-provenance "Malformed source payload envelope")))]
    ;; This format is generated with pr-str. Exact canonical equality also
    ;; rejects trailing forms which read-string would otherwise ignore.
    (when-not (and (map? payload) (= encoded (pr-str payload)))
      (fail! :invalid-provenance "Source payload is not one canonical EDN map"))
    payload))

(defn- source-request [key version]
  {:method :get :path (vendor-path key version) :headers {}})

(defn- document! [document id revision]
  (when-not (and (map? document) (= id (:doc_id document)) (= revision (:revision document))
                 (string? (:title document)) (string? (:content document)))
    (fail! :document-mismatch "CMS document does not match requested revision"))
  (text! "title" (:title document) 200)
  (when (> (count (:content document)) 262144)
    (fail! :invalid-payload "CMS content is too large"))
  document)

(defn- current! [history id revision]
  (let [document (:document history)]
    (when-not (and (map? document) (= id (:doc_id document))
                   (= revision (:revision document))
                   (= [revision] (:revision_heads document))
                   (false? (:conflicted document)))
      (fail! :stale-or-conflicted "Requested revision is not the sole current head"))
    document))

(defn- read-request! [client request]
  (p/catch (p/then (p/resolved nil) (fn [_] (client request)))
           (fn [error] (throw (ex-info "Upstream read failed" {:code :upstream-failure :status 502} error)))))

(defn- get-history! [knoxx id]
  (p/let [response (read-request! knoxx {:method :get :path (str "/api/cms/documents/" id "/history") :headers {}})]
    (:body (response! response "CMS history read"))))

(defn import!
  "Import an explicit source version pinned with ETag into Knoxx. Optional updates
   require the caller's expected revision; concurrent edits can still branch."
  [{:keys [optimizely knoxx]} {:keys [key version field source-system document-id expected-revision] :as request}]
  (p/let [_ (fields! request #{:key :version :field :source-system :document-id :expected-revision})
          _ (when (not= (some? document-id) (some? expected-revision))
              (fail! :invalid-request "Updates require document id and expected revision"))
          _ (when expected-revision (text! "expected revision" expected-revision 1024))
          _ (segment! "source system" source-system)
          _ (segment! "field" field)
          _ (when document-id (segment! "document id" document-id))
          source (read-request! optimizely (source-request key version))
          {:keys [payload etag source-id]} (checked-source! source key version field)
          history (when document-id (get-history! knoxx document-id))
          current (when document-id (:document history))
          _ (when document-id (current! history document-id expected-revision))
          payload-edn (pr-str payload)
          _ (when-not (= payload (decode-payload! "big-pi-edn-v1" payload-edn))
              (fail! :invalid-payload "Source payload cannot be preserved as EDN"))
          provenance {:source-system source-system :source-id source-id :key key :version version :field field
                      :locale (:locale payload) :etag etag :payload-format "big-pi-edn-v1"
                      :payload-edn payload-edn}
          _ (when-not (or (nil? (:metadata current)) (map? (:metadata current)))
              (fail! :invalid-payload "Existing CMS metadata is not a map"))
          metadata (assoc (or (:metadata current) {}) :optimizely provenance)
          _ (when-not (and (map? metadata) (<= (count (pr-str metadata)) 65536))
              (fail! :invalid-payload "Source provenance exceeds CMS metadata limit"))
          body (cond-> {:title (:displayName payload) :content (property payload field)
                        :visibility "internal" :metadata metadata}
                 document-id (assoc :parents [expected-revision]))
          response (p/catch
                    (p/then (p/resolved nil)
                            (fn [_] (knoxx {:method (if document-id :patch :post)
                                            :path (str "/api/cms/documents" (when document-id (str "/" document-id)))
                                            :headers {} :body body})))
                    (fn [error] (throw (ex-info "CMS write outcome uncertain; inspect CMS before retrying"
                                                {:code :outcome-uncertain :status 502} error))))
          _ (when (or (not (map? response)) (not (integer? (:status response)))
                      (>= (:status response) 500))
              (fail! :outcome-uncertain "CMS write outcome uncertain; inspect CMS before retrying"))
          _ (when-not (<= 200 (:status response) 299)
              (response! response "CMS draft write"))
          document (:body response)
          _ (when-not (and (map? document) (string? (:doc_id document))
                   (string? (:revision document)) (not (str/blank? (:revision document)))
                   (vector? (:revision_heads document)) (some #{(:revision document)} (:revision_heads document))
                   (boolean? (:conflicted document))
                   (= (:conflicted document) (> (count (:revision_heads document)) 1))
                   (= (:title body) (:title document)) (= (:content body) (:content document))
                   (= (:metadata body) (:metadata document)) (= "internal" (:visibility document))
                   (or (nil? document-id) (= document-id (:doc_id document))))
      (fail! :outcome-uncertain "CMS write response is inconsistent; inspect CMS before retrying"))]
    {:document document}))

(defn- provenance! [document field]
  (let [{:keys [source-system source-id key version locale etag payload-format payload-edn] :as provenance}
        (get-in document [:metadata :optimizely])]
    (when-not (map? provenance) (fail! :missing-provenance "Optimizely provenance is required"))
    (segment! "source system" source-system)
    (segment! "source identity" source-id)
    (segment! "content key" key)
    (segment! "version" version)
    (when-not (= field (:field provenance)) (fail! :field-mismatch "Field differs from imported field"))
    (locale! locale)
    (text! "ETag" etag 1024)
    (let [payload (decode-payload! payload-format payload-edn)]
      (when-not (and (map? payload) (= key (:key payload)) (= version (:version payload))
                   (= locale (:locale payload)))
      (fail! :invalid-provenance "Stored source coordinate is inconsistent"))
    (text! "source title" (:displayName payload) 200)
      (property payload field)
      (assoc provenance :payload payload))))

(defn export!
  "Create a vendor draft from the explicitly selected sole current Knoxx revision.
   A rejected or ambiguous vendor write is never retried automatically."
  [{:keys [optimizely knoxx]} {:keys [document-id revision target-locale field] :as request}]
  (p/let [_ (fields! request #{:document-id :revision :target-locale :field})
          _ (segment! "document id" document-id)
          _ (text! "revision" revision 1024)
          _ (segment! "field" field)
          _ (locale! target-locale)
          history (get-history! knoxx document-id)
          _ (current! history document-id revision)
          matching (filterv #(= revision (:revision %)) (:revisions history))
          _ (when-not (= 1 (count matching)) (fail! :missing-revision "Exact CMS revision is missing or ambiguous"))
          document (document! (first matching) document-id revision)
          {:keys [key version etag payload locale source-id]} (provenance! document field)
          _ (when (= (str/lower-case locale) (str/lower-case target-locale))
              (fail! :invalid-target-locale "Target locale must differ from source locale"))
          source (read-request! optimizely (source-request key version))
          checked (checked-source! source key version field)
          _ (when-not (= source-id (:source-id checked))
              (fail! :source-identity-mismatch "Source store differs from the imported store"))
          _ (when-not (and (= etag (:etag checked)) (= payload (:payload checked)))
              (fail! :stale-source "Source revision changed since import"))
          ;; Recheck the current CMS head immediately before the write. The remote
          ;; request cannot lock Knoxx: a subsequent edit can still race this call.
          latest (get-history! knoxx document-id)
          _ (current! latest document-id revision)
          request {:method :post :path (str "/v1/content/" key "/versions")
                   :headers {"prefer" "return=representation"}
                   :body {:displayName (:displayName payload) :locale target-locale
                          :properties (assoc (:properties payload) (keyword field) (:content document))}}
          response (p/catch (p/then (p/resolved nil) (fn [_] (optimizely request)))
                            (fn [error] (throw (ex-info "Vendor draft outcome uncertain; inspect vendor before retrying"
                                                        {:code :outcome-uncertain :status 502} error))))
          _ (when (or (not (map? response)) (not (integer? (:status response)))
                      (>= (:status response) 500))
              (fail! :outcome-uncertain "Vendor draft outcome uncertain; inspect vendor before retrying"))
          _ (when-not (<= 200 (:status response) 299)
              (response! response "Vendor draft write"))
          result (:body response)
          _ (when-not (and (map? result) (= key (:key result)) (string? (:version result))
                   (not= version (:version result)) (= target-locale (:locale result))
                   (= (:properties (:body request)) (:properties result))
                   (= "draft" (some-> (:status result) str/lower-case)))
      (fail! :outcome-uncertain "Vendor response did not establish a draft; inspect vendor before retrying"))]
    {:vendor result :source {:key key :version version :source-id source-id} :document-revision revision}))
