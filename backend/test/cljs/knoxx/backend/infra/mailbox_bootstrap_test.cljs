(ns knoxx.backend.infra.mailbox-bootstrap-test
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.infra.actor-mailbox-commands :as commands]
            [knoxx.backend.infra.mailbox-delivery-registry :as deliveries]
            [knoxx.backend.infra.persistence-bootstrap :as bootstrap]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.clio-run-store :as clio-runs]
            [knoxx.backend.infra.stores.mailbox-store :as registry]
            [knoxx.backend.infra.stores.session-store-registry :as runs]
            [knoxx.backend.shape.mailbox-delivery :as delivery]
            [knoxx.backend.shape.mailbox-store :as mailbox]))

(deftest mailbox-selection-is-independent-and-invalid-selection-opens-nothing
  (let [directory (disk/temp-directory!) child (str directory "/mailbox")
        previous @registry/provider* old-delivery @deliveries/provider* old-run @runs/session-store*
        cfg {:wiki-directory directory :mailbox-directory child
             :run-provider :mongodb :thread-provider :mongodb :cache-provider :mongodb :mcp-oauth-provider :mongodb}]
    (try
      (is (thrown? cljs.core.ExceptionInfo (bootstrap/install-local! (assoc cfg :mailbox-provider :mongodb))))
      (is (false? (fs/exists? child)))
      (is (identical? previous @registry/provider*))
      (reset! runs/session-store* (clio-runs/open! {:directory (str directory "/old-runs")}))
      (events/install! @runs/session-store*)
      (is (= :edn (:mailbox-provider (bootstrap/install-local! cfg))))
      (is (nil? @runs/session-store*) "Mongo selection clears the preceding EDN run authority before Mongo starts")
      (is (satisfies? mailbox/IMailboxStore @registry/provider*))
      (is (satisfies? delivery/IMailboxDelivery @deliveries/provider*))
      (is (fs/exists? (str child "/events.edn")))
      (finally
        (reset! runs/session-store* old-run) (events/install! old-run)
        (registry/install! previous) (deliveries/install! old-delivery) (fs/remove-tree! directory)))))

(deftest human-send-controls-respect-explicit-denial-and-each-mode-permission
  (let [context {:actor-binding "actor" :permissions ["agent.chat.use" "agent.controls.follow_up"]
                 :tool-policies [{:tool-id "actors.send-message" :effect "allow"}]}]
    (is (= {:send true :acknowledge true :modes ["inbox-only" "follow-up"]}
           (commands/interface-capabilities context)))
    (is (= {:send false :acknowledge true :modes []}
           (commands/interface-capabilities
            (assoc context :role-slugs ["system_admin"]
                   :tool-policies [{:tool-id "actors.send-message" :effect "deny"}]))))))
