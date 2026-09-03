(ns knoxx.backend.extern.fastify.document-admission-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.fastify.document-admission :as adapter]))

(defn request
  [body]
  (js-obj "body" (clj->js body)))

(deftest admission-body-is-closed-and-selection-is-explicit
  (is (= {:anchors? true}
         (adapter/decode-request (request {}))))
  (is (= {:anchors? false :document :knoxx.docs/probe}
         (adapter/decode-request
          (request {:document "knoxx.docs/probe"}))))
  (is (= {:anchors? true :generate-drafts? true}
         (adapter/decode-request
          (request {:anchors true :generateDrafts true}))))
  (testing "a typo cannot become an anchor sweep"
    (is (thrown? js/Error
                 (adapter/decode-request (request {:anchor true})))))
  (testing "an unqualified exact identity is refused"
    (is (thrown? js/Error
                 (adapter/decode-request (request {:document "probe"})))))
  (testing "two positive selectors are ambiguous and refused"
    (is (thrown? js/Error
                 (adapter/decode-request
                  (request {:anchors true
                            :document "knoxx.docs/probe"}))))))

(def document
  {:document/id :knoxx.docs/probe
   :document/title "Probe"
   :document/source-locale :en
   :document/source {:path "docs/probe.md"}
   :document/anchor? true})

(def garden
  {:garden/id :knoxx.gardens/main
   :garden/title "Main"
   :garden/status :active
   :garden/locales [:en :es]})

(def publication
  {:publication/id :knoxx.publications/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.gardens/main
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :draft
   :publication/path "/probe-es"
   :translation/review :required})

(def resource-records
  [{:ok? true
    :resource/kind :document
    :resource/file-path "/workspace/contracts/document.edn"
    :resource/definition document}
   {:ok? true
    :resource/kind :garden
    :resource/file-path "/workspace/contracts/garden.edn"
    :resource/definition garden}
   {:ok? true
    :resource/kind :publication
    :resource/file-path "/workspace/contracts/publication.edn"
    :resource/definition publication}])

(defn reply
  [response]
  (let [reply (js-obj)]
    (aset reply "code" (fn [status]
                          (swap! response assoc :status status)
                          reply))
    (aset reply "type" (fn [_] reply))
    (aset reply "send" (fn [body]
                          (swap! response assoc
                                 :body (js->clj body :keywordize-keys true))
                          reply))
    (aset reply "sent" false)
    reply))

(deftest ^:async registered-route-authorizes-and-auto-dispatches
  (let [routes (atom [])
        app (js-obj "route" (fn [options] (swap! routes conj options)))
        response (atom {})
        checks (atom [])
        dispatches (atom [])
        ctx {:org-id "org-1" :membership-id "member-1"}
        handlers {:with-request-context! (fn [_ _ _ operation]
                                           (operation ctx))
                  :ensure-permission! (fn [actual permission]
                                        (swap! checks conj
                                               [actual permission]))}
        dependencies
        {:resource-records! (fn [_]
                              (js/Promise.resolve resource-records))
         :document-source-roots (fn [_ _]
                                  {:knoxx.docs/probe "/workspace"})
         :canonical-document-path!
         (fn [root doc]
           (js/Promise.resolve
            (str root "/" (get-in doc [:document/source :path]))))
         :source-content! (fn [_ _]
                            (js/Promise.resolve "# Probe"))
         :persist-event! (fn [event]
                           (js/Promise.resolve {:ok true :ids [(:id event)]}))
         :emit-indexed! (fn [_]
                          (js/Promise.resolve {:matchedTriggers []}))
         :dispatch-document! (fn [document-id _snapshot-deps]
                               (swap! dispatches conj document-id)
                               (js/Promise.resolve
                                {:considered 1
                                 :admissible 1
                                 :runner :agent
                                 :dispatched []}))
         :clock (constantly "2026-09-02T12:00:00.000Z")
         :digest-hex (fn [value] (str "digest-" (hash value)))}]
    (adapter/register-document-admission-routes!
     app {} {:session-project-name "knoxx-local"}
     handlers dependencies)
    (let [route (first @routes)]
      (await ((aget route "handler")
              (request {:anchors true :generateDrafts false})
              (reply response)))
      (is (= "POST" (aget route "method")))
      (is (= "/api/publications/documents/admit" (aget route "url")))
      (is (= [[ctx adapter/admission-permission]] @checks))
      (is (= [:knoxx.docs/probe] @dispatches))
      (is (= 200 (:status @response)))
      (is (true? (get-in @response [:body :ok])))
      (is (= 1 (get-in @response [:body :admitted])))
      (is (= 0 (get-in @response [:body :failed]))))))

(deftest ^:async explicit-or-policy-draft-generation-requires-publication-management-before-effects
  (let [routes (atom [])
        app (js-obj "route" (fn [options] (swap! routes conj options)))
        explicit-response (atom {})
        policy-response (atom {})
        checks (atom [])
        effects (atom [])
        ctx {:org-id "org-1" :membership-id "member-1"}
        handlers {:with-request-context! (fn [_ _ _ operation]
                                           (operation ctx))
                  :ensure-permission! (fn [actual permission]
                                        (swap! checks conj [actual permission])
                                        (when (= adapter/draft-permission permission)
                                          (throw (ex-info "forbidden"
                                                          {:status 403}))))}
        dependencies
        {:resource-records! (fn [_]
                              (swap! effects conj :resource-read)
                              (js/Promise.resolve resource-records))
         :dispatch-document! (fn [_ _]
                               (swap! effects conj :dispatch)
                               (js/Promise.resolve {}))}]
    (adapter/register-document-admission-routes!
     app {} {:session-project-name "knoxx-local"}
     handlers dependencies)
    (let [handler (aget (first @routes) "handler")]
      (await (handler (request {:anchors true :generateDrafts true})
                      (reply explicit-response)))
      (await (handler (request {:anchors true})
                      (reply policy-response))))
    (is (= [[ctx adapter/admission-permission]
            [ctx adapter/draft-permission]
            [ctx adapter/admission-permission]
            [ctx adapter/draft-permission]]
           @checks))
    (is (= 403 (:status @explicit-response)))
    (is (= 403 (:status @policy-response)))
    (is (empty? @effects)
        "authorization fails before resources, events, or agents are touched")))
