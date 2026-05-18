(ns knoxx.backend.memory-routes-test
  (:require [cljs.test :refer [async deftest is]]
            [knoxx.backend.routes.memory :as memory-routes]))

(defn- offset-from-path
  [path]
  (let [url (js/URL. (str "http://localhost" path))
        offset (js/parseInt (or (.get (.-searchParams url) "offset") "0") 10)]
    (if (js/Number.isFinite offset) offset 0)))

(deftest session-list-limit-is-bounded
  (is (= 12 (memory-routes/session-list-limit nil)))
  (is (= 20 (memory-routes/session-list-limit "20")))
  (is (= memory-routes/max-session-list-page-size
         (memory-routes/session-list-limit "500"))))

(deftest session-list-upstream-page-size-is-bounded
  (is (= 21 (memory-routes/session-list-upstream-page-size 20 0)))
  (is (= memory-routes/max-session-list-upstream-page-size
         (memory-routes/session-list-upstream-page-size 80 0)))
  (is (= memory-routes/max-session-list-upstream-page-size
         (memory-routes/session-list-upstream-page-size 40 80))))

(deftest memory-sessions-cache-key-is-param-and-auth-scoped
  (let [base {:config {:session-project-name "knoxx"}
              :ctx {:orgId "org-1"
                    :membershipId "member-1"
                    :userId "user-1"
                    :actorId "actor-1"
                    :permissions ["agent.memory.cross_session"]}
              :limit 20
              :offset 0
              :actor-id "chat_primary"
              :exclude-actor-ids ["eta-mu" "pi"]
              :contract-id "fork_tales_creative_director"}]
    (is (= (memory-routes/memory-sessions-cache-key base)
           (memory-routes/memory-sessions-cache-key (assoc base :exclude-actor-ids ["pi" "eta-mu"]))))
    (is (not= (memory-routes/memory-sessions-cache-key base)
              (memory-routes/memory-sessions-cache-key (assoc-in base [:ctx :userId] "user-2"))))
    (is (not= (memory-routes/memory-sessions-cache-key base)
              (memory-routes/memory-sessions-cache-key (assoc base :actor-id "eta-mu"))))
    (is (not= (memory-routes/memory-sessions-cache-key base)
              (memory-routes/memory-sessions-cache-key (assoc base :contract-id "other_agent"))))))

(deftest cached-memory-sessions-source-coalesces-and-reuses-local-cache
  (async done
    (memory-routes/clear-memory-sessions-cache!)
    (let [calls* (atom 0)
          cache-key "test-coalesce"
          fetch-fn (fn []
                     (swap! calls* inc)
                     (js/Promise.
                      (fn [resolve _reject]
                        (js/setTimeout
                         (fn []
                           (resolve {:rows [{:session "s1"}]
                                     :has_more false}))
                         0))))]
      (-> (.all js/Promise
                (clj->js [(memory-routes/cached-memory-sessions-source! nil cache-key fetch-fn)
                          (memory-routes/cached-memory-sessions-source! nil cache-key fetch-fn)]))
          (.then (fn [results]
                   (let [results (js->clj results :keywordize-keys true)]
                     (is (= 1 @calls*))
                     (is (= [{:session "s1"}] (get-in (first results) [:value :rows])))
                     (is (= "miss" (get-in (first results) [:cache :tier]))))
                   (memory-routes/cached-memory-sessions-source! nil cache-key fetch-fn)))
          (.then (fn [cached]
                   (let [cached (js->clj cached :keywordize-keys true)]
                     (is (= 1 @calls*))
                     (is (= "memory" (get-in cached [:cache :tier])))
                     (is (true? (get-in cached [:cache :hit]))))))
          (.catch (fn [err]
                    (is nil (str "unexpected rejection: " err))))
          (.finally (fn []
                      (memory-routes/clear-memory-sessions-cache!)
                      (done)))))))

(deftest fetch-authorized-session-pages-stops-after-needed-window
  (async done
    (let [page-offsets* (atom [])
          session-row-fetches* (atom [])
          pages {0 {:rows [{:session "s1"}] :has_more true}
                 1 {:rows [{:session "s2"}] :has_more true}
                 2 {:rows [{:session "s3"}] :has_more true}
                 3 {:rows [{:session "s4"}] :has_more false}}
          openplanner-request! (fn [_config _method path]
                                 (let [offset (offset-from-path path)]
                                   (swap! page-offsets* conj offset)
                                   (js/Promise.resolve (get pages offset {:rows [] :has_more false}))))
          authorized-session-ids! (fn [_config _ctx session-ids]
                                    (js/Promise.resolve (set (map str session-ids))))
          fetch-openplanner-session-rows! (fn [_config session-id]
                                            (swap! session-row-fetches* conj session-id)
                                            (js/Promise.resolve [{:extra "{}"}]))
          session-matches-page-actor-filter? (fn [_config _rows _actor-id _exclude-actor-ids] true)]
      (-> (memory-routes/fetch-authorized-session-pages! {} {} "chat_primary"
                                                         []
                                                         nil
                                                         openplanner-request!
                                                         authorized-session-ids!
                                                         fetch-openplanner-session-rows!
                                                         session-matches-page-actor-filter?
                                                         1
                                                         0
                                                         []
                                                         3)
          (.then (fn [result]
                   (let [result (js->clj result :keywordize-keys true)]
                     (is (= [0 1 2] @page-offsets*))
                     (is (= ["s1" "s2" "s3"] @session-row-fetches*))
                     (is (= ["s1" "s2" "s3"] (mapv :session (:rows result))))
                     (is (true? (:has_more result))))))
          (.catch (fn [err]
                    (is nil (str "unexpected rejection: " err))))
          (.finally (fn [] (done)))))))

(deftest fetch-authorized-session-pages-honors-excluded-actors
  (async done
    (let [pages {0 {:rows [{:session "s1"} {:session "s2"}] :has_more false}}
          openplanner-request! (fn [_config _method _path]
                                 (js/Promise.resolve (get pages 0)))
          authorized-session-ids! (fn [_config _ctx session-ids]
                                    (js/Promise.resolve (set (map str session-ids))))
          fetch-openplanner-session-rows! (fn [_config session-id]
                                            (js/Promise.resolve [{:extra (if (= session-id "s1")
                                                                           "{\"actor_id\":\"pi\"}"
                                                                           "{\"actor_id\":\"chat_primary\"}")}]))
          session-matches-page-actor-filter? (fn [_config rows _actor-id exclude-actor-ids]
                                               (not-any? #(= "pi" %) exclude-actor-ids)
                                               (not= "pi"
                                                     (some-> rows first :extra js/JSON.parse (aget "actor_id"))))]
      (-> (memory-routes/fetch-authorized-session-pages! {} {} nil
                                                         ["pi"]
                                                         nil
                                                         openplanner-request!
                                                         authorized-session-ids!
                                                         fetch-openplanner-session-rows!
                                                         session-matches-page-actor-filter?
                                                         10
                                                         0
                                                         []
                                                         10)
          (.then (fn [result]
                   (let [result (js->clj result :keywordize-keys true)]
                     (is (= ["s2"] (mapv :session (:rows result)))))))
          (.catch (fn [err]
                    (is nil (str "unexpected rejection: " err))))
          (.finally (fn [] (done)))))))

(deftest fetch-authorized-session-pages-honors-contract-id
  (async done
    (let [pages {0 {:rows [{:session "s1"} {:session "s2"}] :has_more false}}
          openplanner-request! (fn [_config _method _path]
                                 (js/Promise.resolve (get pages 0)))
          authorized-session-ids! (fn [_config _ctx session-ids]
                                    (js/Promise.resolve (set (map str session-ids))))
          fetch-openplanner-session-rows! (fn [_config session-id]
                                            (js/Promise.resolve [{:extra (if (= session-id "s2")
                                                                           "{\"contract_id\":\"fork_tales_creative_director\",\"actor_id\":\"agent_librarian\"}"
                                                                           "{\"contract_id\":\"other_agent\"}")}]))
          session-matches-page-actor-filter? (fn [_config _rows _actor-id _exclude-actor-ids] true)]
      (-> (memory-routes/fetch-authorized-session-pages! {} {} nil
                                                         []
                                                         "fork_tales_creative_director"
                                                         openplanner-request!
                                                         authorized-session-ids!
                                                         fetch-openplanner-session-rows!
                                                         session-matches-page-actor-filter?
                                                         10
                                                         0
                                                         []
                                                         10)
          (.then (fn [result]
                   (let [result (js->clj result :keywordize-keys true)]
                     (is (= ["s2"] (mapv :session (:rows result))))
                     (is (= "fork_tales_creative_director" (-> result :rows first :contract_id))))))
          (.catch (fn [err]
                    (is nil (str "unexpected rejection: " err))))
          (.finally (fn [] (done)))))))
