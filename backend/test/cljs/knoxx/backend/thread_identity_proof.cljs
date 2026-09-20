(ns knoxx.backend.thread-identity-proof
  "Immutable thread identity scenarios and controlled native admission races."
  (:require [cljs.test :refer [is]]
            [knoxx.backend.extern.mongo-thread :as native]
            [knoxx.backend.extern.provider-recovery-fixture :as concurrent]
            [knoxx.backend.shape.thread-store :as protocol]))

(defn- ^:async outcome! [operation]
  (try {:value (await (operation))}
       (catch :default error {:error (ex-data error)})))

(defn ^:async check-rebinding!
  "Both mutation entry points preserve every established owner field."
  [provider]
  (let [original {:session_id "bound" :conversation_id "conversation-a" :org_id "org-a"
                  :user_id "user-a" :status "completed" :messages [{:role "user" :content "Private"}]}]
    (await (protocol/put-thread! provider original))
    (doseq [method [:put :patch] field [:conversation_id :org_id :user_id] value ["foreign" nil]]
      (let [delta {field value}
            result (await (outcome! #(if (= method :put)
                                       (protocol/put-thread! provider (assoc delta :session_id "bound"))
                                       (protocol/patch-thread! provider "bound" delta))))]
        (is (= "thread_store_identity_conflict" (get-in result [:error :code])) (str method " cannot replace " field))
        (is (= original (select-keys (await (protocol/read-thread provider "bound")) (keys original))))))
    (let [same (await (protocol/patch-thread! provider "bound" {:org_id "org-a" :status "running"}))]
      (is (= "org-a" (:org_id same)))
      (is (= "running" (:status same))))))

(defn ^:async check-initial-assignment!
  "Conflicting simultaneous initial owner assignments admit one complete winner."
  [provider]
  (doseq [seed? [true false] method [:put :patch] field [:conversation_id :org_id :user_id]]
    (let [id (str seed? "-" (name method) "-" (name field))
          _ (when seed? (await (protocol/put-thread! provider {:session_id id :status "waiting_input"})))
          deltas [{field (str id "-a") :winner "a"} {field (str id "-b") :winner "b"}]
          results (await (concurrent/settled
                           (mapv #(if (= method :put)
                                    (protocol/put-thread! provider (assoc % :session_id id))
                                    (protocol/patch-thread! provider id %)) deltas)))
          rejected (filter #(= :rejected (:status %)) results)
          stored (await (protocol/read-thread provider id))]
      (is (= 1 (count (filter #(= :fulfilled (:status %)) results))))
      (is (= ["thread_store_identity_conflict"] (mapv #(get-in % [:error :code]) rejected)))
      (is (= (str id "-" (:winner stored)) (get stored field))))))

(defn ^:async check-invalid-identity!
  "Malformed and native-encoding aliases cannot bypass identity guards."
  [provider]
  (let [original {:session_id "canonical" :org_id "org-a" :user_id "user-a" :status "completed"}]
    (await (protocol/put-thread! provider original))
    (doseq [method [:put :patch]
            delta [{:org_id []} {:user_id {}} {:org_id ""} {"org_id" "foreign"}
                   {:alias/org_id "foreign"} {:org_id.child "foreign"}]]
      (let [result (await (outcome! #(if (= method :put)
                                     (protocol/put-thread! provider (assoc delta :session_id "canonical"))
                                     (protocol/patch-thread! provider "canonical" delta))))]
        (is (= "thread_store_invalid" (get-in result [:error :code])))
        (is (= original (select-keys (await (protocol/read-thread provider "canonical")) (keys original))))))))

(defn ^:async check-compatible-and-unique!
  "Matching concurrent owners succeed; a conversation belongs to only one live thread."
  [provider]
  (let [results (await (concurrent/settled
                        [(protocol/put-thread! provider {:session_id "compatible" :conversation_id "compatible-conversation" :org_id "same" :user_id "same-user" :left true})
                         (protocol/put-thread! provider {:session_id "compatible" :conversation_id "compatible-conversation" :org_id "same" :user_id "same-user" :right true})]))
        written (await (protocol/read-thread provider "compatible"))]
    (is (every? #(= :fulfilled (:status %)) results))
    (is (= {:org_id "same" :left true :right true} (select-keys written [:org_id :left :right]))))
  (await (protocol/put-thread! provider {:session_id "unique-a" :conversation_id "unique-conversation"}))
  (let [result (await (outcome! #(protocol/put-thread! provider {:session_id "unique-b" :conversation_id "unique-conversation"})))]
    (is (= "thread_store_conversation_conflict" (get-in result [:error :code])))
    (is (nil? (await (protocol/read-thread provider "unique-b")))))
  (doseq [id ["unbound-a" "unbound-b"]]
    (let [written (await (protocol/put-thread! provider {:session_id id :conversation_id nil}))]
      (is (= id (:session_id written)))
      (is (nil? (:conversation_id written))))))

(defn- ^:async replace-once! [done db provider replacement]
  (when-not @done
    (reset! done true)
    (await (native/delete-session! db (:session_id replacement)))
    (await (protocol/put-thread! provider replacement))))

(defn- ^:async recreated-owner-case! [provider db operation field binding]
  (let [id (str "aba-" (name operation) "-" (name field) "-" (name binding))
        original (cond-> {:session_id id :org_id "org-before" :user_id "user-before"
                          :conversation_id id :status "completed" :messages [{:role "user" :content "Same bytes"}]}
                   (= binding :null) (assoc field nil)
                   (= binding :missing) (dissoc field))
        replacement (assoc original field (str id "-replacement"))
        done (atom false) upsert native/upsert-session! rewind native/patch-if-messages!]
    (await (protocol/put-thread! provider original))
    (with-redefs [native/upsert-session! (^:async fn [handle fields observed]
                                          (when-not (= operation :rewind) (await (replace-once! done db provider replacement)))
                                          (await (upsert handle fields observed)))
                  native/patch-if-messages! (^:async fn [handle observed updates]
                                             (await (replace-once! done db provider replacement))
                                             (await (rewind handle observed updates)))]
      (let [result (await (outcome! #(case operation
                                      :put (protocol/put-thread! provider {:session_id id :status "running" :stale_marker true})
                                      :patch (protocol/patch-thread! provider id {:status "running" :stale_marker true})
                                      :rewind (protocol/rewind-thread! provider id 1))))]
        (is (= "thread_store_identity_conflict" (get-in result [:error :code])))
        (is (= 409 (get-in result [:error :status])))))
    (let [stored (await (protocol/read-thread provider id))]
      (is (= replacement (select-keys stored (keys replacement))))
      (is (not (contains? stored :stale_marker))))))

(defn ^:async check-recreated-owner!
  "A stale mutation cannot touch a recreated owner's thread with identical messages."
  [provider db]
  (doseq [operation [:put :patch :rewind] field [:conversation_id :org_id :user_id]
          binding (if (= operation :rewind) [:bound :null :missing] [:bound])]
    (await (recreated-owner-case! provider db operation field binding))))
