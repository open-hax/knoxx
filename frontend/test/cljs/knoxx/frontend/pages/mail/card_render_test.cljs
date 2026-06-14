(ns knoxx.frontend.pages.mail.card-render-test
  "Written FIRST (TDD) — render contract for the mailbox-card Helix
  component (port of MailboxCard in src/pages/MailPage.tsx). Navigation
  is injected as :on-navigate so the card renders without a Router."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            ["react-dom/server" :as rds]
            [helix.core :refer [$]]
            [knoxx.frontend.pages.mail.card :refer [mailbox-card]]))

(defn render [props]
  ;; {& props} — a bare non-literal map would be treated as a child, not props
  (rds/renderToStaticMarkup ($ mailbox-card {& props})))

(def pending-entry
  {:id "m1"
   :status "pending"
   :source {:actor-id "agent-a"}
   :target {:actorId "user-b"}
   :delivery {:mode "handoff" :attempts 2}
   :contentRef {:run-id "r1"}
   :preview "Work is ready for review."
   :createdAt "2026-01-05T10:00:00Z"
   :lastError "boom"})

(deftest renders-status-route-and-preview
  (let [html (render {:entry pending-entry :box "inbox"
                      :acking false :on-ack (fn [_]) :on-navigate (fn [_])})]
    (is (str/includes? html "pending"))
    (is (str/includes? html "amber") "status chip uses the pending tone")
    (is (str/includes? html "agent-a"))
    (is (str/includes? html "user-b"))
    (is (str/includes? html "handoff"))
    (is (str/includes? html "attempts 2"))
    (is (str/includes? html "Work is ready for review."))
    (is (str/includes? html "Open run") "contentRef run-id yields a link button")
    (is (str/includes? html "boom") "lastError is surfaced")))

(deftest acknowledge-button-visibility
  (testing "inbox + not acknowledged → button"
    (is (str/includes?
         (render {:entry pending-entry :box "inbox"
                  :acking false :on-ack (fn [_]) :on-navigate (fn [_])})
         "Acknowledge")))
  (testing "acknowledged entries get no button"
    (is (not (str/includes?
              (render {:entry (assoc pending-entry :status "acknowledged")
                       :box "inbox" :acking false :on-ack (fn [_]) :on-navigate (fn [_])})
              "Acknowledge"))))
  (testing "outbox gets no button"
    (is (not (str/includes?
              (render {:entry pending-entry :box "outbox"
                       :acking false :on-ack (fn [_]) :on-navigate (fn [_])})
              "Acknowledge")))))

(deftest fallback-preview-and-unknown-parties
  (let [html (render {:entry {:id "m2" :status "delivered"
                              :source {} :target {} :delivery {} :contentRef {}}
                      :box "outbox" :acking false :on-ack (fn [_]) :on-navigate (fn [_])})]
    (is (str/includes? html "No preview available"))
    (is (str/includes? html "unknown"))))
