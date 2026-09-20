(ns knoxx.backend.infra.clients.openplanner-clio-test
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.local-openplanner-events :as events]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.extern.local-openplanner :as host]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.clients.openplanner :as client]
            [knoxx.backend.infra.clients.openplanner-clio :as local]
            [knoxx.backend.infra.clio-application-store :as engine]))

(def scope {:org_id "org-a" :project "wiki"})
(def embedding-config {:embed-provider-base-url "http://127.0.0.1:9999/v1"
                       :embed-provider-model "fixture-model" :embed-provider-dimensions 3})
(defn- fake-embed [texts]
  {:model "fixture-model" :dimensions 3 :vectors (mapv #(if (= % "Other") [0 1 0] [1 0 0]) texts)})
(defn- options [root]
  {:directory root :embedding-config embedding-config :embed! fake-embed
   :now! (constantly "2026-09-12T12:00:00.000Z")})
(defn- event [id org text]
  {:id id :text text :source "knoxx" :kind "message" :ts "2026-09-12T12:00:00.000Z"
   :source_ref {:project "wiki" :session "conversation"} :meta {:role "user"}
   :extra {:org_id org :owner "user-a"}})
(defn- ^:async refused [operation]
  (try (await (operation)) nil (catch :default error (ex-data error))))
(defn- ^:async fixture! [operation]
  (let [root (disk/temp-directory!)]
    (try
      (await (operation root))
      (catch :default error (is false (str "Unexpected local OpenPlanner test failure: " error)))
      (finally (fs/remove-tree! root)))))

(deftest ^:async event-facts-and-real-projection-contract-survive-restart
  (await (fixture!
    (^:async fn [root]
      (let [provider (local/open! (options root))
            a (event "event-a" "org-a" "Hello")
            b (event "event-b" "org-b" "Other")]
        (is (= 2 (:count (await (client/events! provider [a b])))))
        (is (= 0 (:count (await (client/events! provider [a b])))))
        (let [restarted (local/open! (options root))
              detail (await (client/session! restarted "conversation" scope))
              search (await (client/vector-search! restarted (assoc scope :q "Hello")))
              summaries (await (client/sessions! restarted scope))]
          (is (= ["event-a"] (mapv :id (:rows detail))))
          (is (= ["event-a"] (get-in search [:result :ids 0])))
          (is (= "conversation" (get-in summaries [:rows 0 :session])))
          (is (= a (await (client/event-by-id! restarted "event-a"))))
          (is (= ["event-a"] (:event-ids (await (client/ensure-event-vectors! restarted ["event-a"])))))
          (is (= "openplanner_event_conflict"
                 (:code (await (refused #(client/events! restarted [(assoc a :text "Changed")]))))))))))))

(deftest ^:async no-model-startup-retains-facts-and-repairs-after-configuration
  (await (fixture!
    (^:async fn [root]
      (let [provider (local/open! {:directory root})
            value (event "pending" "org-a" "Hello")]
        (is (false? (get-in (await (client/health! provider)) [:body :embeddings-configured])))
        (is (= "openplanner_embedding_not_configured" (:code (await (refused #(client/events! provider [value]))))))
        (is (= value (await (client/event-by-id! provider "pending"))))
        (is (= 1 (count (:rows (await (client/session! provider "conversation" scope))))))
        (is (= "openplanner_embedding_not_configured"
               (:code (await (refused #(client/vector-search! provider (assoc scope :q "Hello")))))))
        (let [configured (local/open! (options root))]
          (is (= ["pending"] (:repaired-event-ids (await (client/ensure-event-vectors! configured ["pending"])))))
          (is (= [] (:repaired-event-ids (await (client/ensure-event-vectors! configured ["pending"])))))
          (is (= [["pending"]] (get-in (await (client/vector-search! configured (assoc scope :q "Hello"))) [:result :ids])))))))))

(deftest ^:async scopes-are-validated-before-model-access-even-with-empty-ledger
  (await (fixture!
    (^:async fn [root]
      (let [calls (atom 0)
            provider (local/open! (assoc (options root) :embed! (fn [texts] (swap! calls inc) (fake-embed texts))))]
        (doseq [request [{} {:org_id ""} {:org_id 7}]]
          (is (= 403 (:status (await (refused #(client/session! provider "conversation" request))))))
          (is (= 403 (:status (await (refused #(client/sessions! provider request))))))
          (is (= 403 (:status (await (refused #(client/vector-search! provider (assoc request :q "Hello"))))))))
        (is (zero? @calls))
        (is (= "openplanner_local_operation_unsupported"
               (:code (await (refused #(client/mongo-query! provider {:filter {}})))))))))))

(deftest ^:async partial-settings-invalid-clock-and-invalid-vector-fail-honestly
  (await (fixture!
    (^:async fn [root]
      (is (= 400 (:status (await (refused #(local/open! {:directory (str root "/partial")
                                                       :embedding-config {:embed-provider-model "only-model"}}))))))
      (is (false? (fs/exists? (str root "/partial"))))
      (is (= "openplanner_timestamp_invalid"
             (:code (await (refused #(local/open! {:directory (str root "/clock") :now! (constantly "invalid")}))))))
      (is (false? (fs/exists? (str root "/clock"))))
      (let [provider (local/open! (assoc (options (str root "/vectors"))
                                       :embed! (fn [_] {:model "fixture-model" :dimensions 3 :vectors [[1 ##NaN 0]]})))
            value (event "invalid-vector" "org-a" "Hello")]
        (is (some? (await (refused #(client/events! provider [value])))))
        (is (= value (await (client/event-by-id! provider "invalid-vector"))))
        (is (= 1 (count (engine/history (:ledger provider)))))
        (is (= "1970-01-01T00:00:00.000Z" (:ts (host/normalize-event (assoc value :ts 0))))))))))

(def segment-input
  {:org_id "org-a" :project "wiki" :garden_id "garden/a" :source_text "Source"
   :translated_text "Candidate" :source_lang "en" :target_lang "es" :document_id "docs/a"
   :segment_index 0 :status "in_review"})
(def review-input
  {:org_id "org-a" :project "wiki" :garden_id "garden/a" :adequacy "good" :fluency "good"
   :terminology "correct" :risk "safe" :overall "approve" :labeler_id "reviewer"})

(deftest ^:async new-candidate-generation-invalidates-old-reviews-without-erasing-history
  (await (fixture!
    (^:async fn [root]
      (let [provider (local/open! {:directory root})
            old-id (:id (await (client/create-translation-segment! provider segment-input)))
            scoped (assoc scope :garden_id "garden/a")]
        (is (= "approved" (:new_status (await (client/label-translation-segment! provider old-id review-input)))))
        (is (= "approved" (:status (await (client/create-translation-segment! provider segment-input)))))
        (let [new-input (assoc segment-input :translated_text "New candidate")
              new-id (:id (await (client/create-translation-segment! provider new-input)))
              restarted (local/open! {:directory root})
              document (await (client/translation-document! restarted "docs/a" "es" scoped))]
          (is (not= old-id new-id))
          (is (= 0 (get-in document [:summary :approved])))
          (is (= [new-id] (mapv :id (:segments document))))
          (is (= 1 (count (:labels (await (client/translation-segment! restarted old-id scoped))))))
          (is (= 409 (:status (await (refused #(client/label-translation-segment! restarted old-id review-input))))))
          (is (= 409 (:status (await (refused #(client/create-translation-segment! restarted segment-input))))))
          (is (= 404 (:status (await (refused #(client/translation-segment! restarted new-id (assoc scoped :org_id "org-b")))))))))))))

(deftest ^:async document-reviews-and-manifests-replay-as-one-accepted-operation
  (await (fixture!
    (^:async fn [root]
      (let [provider (local/open! {:directory root})
            scoped (assoc scope :garden_id "garden/a")
            first-id (:id (await (client/create-translation-segment! provider segment-input)))]
        (await (client/create-translation-segment! provider (assoc segment-input :segment_index 1 :source_text "Second")))
        (let [result (await (client/review-translation-document!
                             provider "docs/a" "es"
                             (assoc scoped :overall "approve" :segment_overrides {first-id {:corrected_text "Corrected"}})))
              restarted (local/open! {:directory root})
              document (await (client/translation-document! restarted "docs/a" "es" scoped))
              manifest (await (client/translation-export-manifest! restarted scope))]
          (is (= 2 (:segments_reviewed result)))
          (is (= 0 (:segments_failed result)))
          (is (= 2 (:graph_memory_failures result)))
          (is (= 2 (get-in document [:summary :approved])))
          (is (= "Corrected" (get-in document [:segments 0 :translated_text])))
          (is (= 2 (get-in manifest [:languages "es" :approved])))
          (is (= 1 (get-in manifest [:languages "es" :with_corrections])))
          (is (= 1 (:total (await (client/translation-documents! restarted scoped)))))
          (is (string? (await (client/translation-export-sft! restarted scope))))))))))

(deftest ^:async atomic-batch-claim-prevents-two-workers-winning-one-job
  (await (fixture!
    (^:async fn [root]
      (let [provider (local/open! {:directory root})
            other (local/open! {:directory root})
            request {:org_id "org-a" :project "wiki" :garden_id "garden/a" :target_lang "es"
                     :document_ids ["docs/a"] :membership_id "member-a" :dispatch_key "dispatch-a"}
            created (await (client/create-translation-batch! provider request))]
        (is (= (:batch_id created) (:batch_id (await (client/create-translation-batch! provider request)))))
        (is (= 409 (:status (await (refused #(client/create-translation-batch! provider (assoc request :document_ids ["docs/b"])))))))
        (let [attempt (^:async fn [store]
                        (try (await (client/next-translation-batch! store scope))
                             (catch :default error {:error (ex-data error)})))
              results (await (promise/all-vec [(attempt provider) (attempt other)]))]
          (is (= 1 (count (filter :batch results))))
          (is (every? #(or (:batch %) (nil? (:error %)) (= 409 (get-in % [:error :status]))) results)))
        (is (nil? (:batch (await (client/next-translation-batch! provider scope)))))
        (is (nil? (:membership_id (await (client/translation-batch! provider (:batch_id created) scope)))))
        (is (= 404 (:status (await (refused #(client/translation-batch! provider (:batch_id created) {:org_id "org-b"})))))))))))

(deftest vector-facts-cannot-change-the-source-digest
  (testing "Pure replay refuses finite vectors bound to different event text"
    (is (thrown? cljs.core/ExceptionInfo
                 (events/project-vectors {:events {"one" (event "one" "org-a" "Hello")}}
                                         [{:id "one" :digest "wrong" :model "fixture-model"
                                           :dimensions 3 :embedding [1 0 0]}])))))
