(ns knoxx.frontend.pages.mail.card-render-test
  "Written FIRST (TDD) — render contract for the mailbox-card Helix
  component (port of MailboxCard in src/pages/MailPage.tsx). Navigation
  is injected as :on-navigate so the card renders without a Router."
  (:require ["react-dom/server" :as rds]
            [cljs.test :as t]
            [clojure.string :as str]
            [helix.core :as hx]
            [knoxx.frontend.pages.mail.card :as card]))

(defn- render [props]
  ;; {& props} — a bare non-literal map would be treated as a child, not props
  (rds/renderToStaticMarkup (hx/$ card/mailbox-card {& props})))

(def ^:private pending-entry
  {:id "m1"
   :status "pending"
   :source {:actor-id "agent-a"}
   :target {:actorId "user-b"}
   :delivery {:mode "handoff" :attempts 2}
   :contentRef {:run-id "r1"}
   :preview "Work is ready for review."
   :createdAt "2026-01-05T10:00:00Z"
   :lastError "boom"})

(t/deftest renders-status-route-and-preview
  (let [html (render {:entry pending-entry :box "inbox"
                      :can-ack? true :acking false :on-ack (fn [_]) :on-navigate (fn [_])})]
    (t/is (str/includes? html "pending"))
    (t/is (str/includes? html "amber") "status chip uses the pending tone")
    (t/is (str/includes? html "agent-a"))
    (t/is (str/includes? html "user-b"))
    (t/is (str/includes? html "handoff"))
    (t/is (str/includes? html "attempts 2"))
    (t/is (str/includes? html "Work is ready for review."))
    (t/is (str/includes? html "Open run") "contentRef run-id yields a link button")
    (t/is (str/includes? html "boom") "lastError is surfaced")))

(t/deftest acknowledge-button-visibility
  (t/testing "inbox + not acknowledged → button"
    (t/is (str/includes?
         (render {:entry pending-entry :box "inbox"
                  :can-ack? true :acking false :on-ack (fn [_]) :on-navigate (fn [_])})
         "Acknowledge")))
  (t/testing "acknowledged entries get no button"
    (t/is (not (str/includes?
              (render {:entry (assoc pending-entry :status "acknowledged")
                       :box "inbox" :can-ack? true :acking false :on-ack (fn [_]) :on-navigate (fn [_])})
              "Acknowledge"))))
  (t/testing "outbox gets no button"
    (t/is (not (str/includes?
              (render {:entry pending-entry :box "outbox"
                       :can-ack? true :acking false :on-ack (fn [_]) :on-navigate (fn [_])})
              "Acknowledge")))))

(t/deftest fallback-preview-and-unknown-parties
  (let [html (render {:entry {:id "m2" :status "delivered"
                              :source {} :target {} :delivery {} :contentRef {}}
                      :box "outbox" :can-ack? true :acking false :on-ack (fn [_]) :on-navigate (fn [_])})]
    (t/is (str/includes? html "No preview available"))
    (t/is (str/includes? html "unknown"))))
