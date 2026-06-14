(ns knoxx.frontend.pages.mail.logic-test
  "Written FIRST (TDD) — pure-logic contract for the Helix port of
  src/pages/MailPage.tsx (record-string, status-tone, format-date,
  find-string-deep, mailbox-links, unread-count) plus the mailbox
  normalizers from src/lib/api/runtime.ts."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.frontend.pages.mail.logic :as logic]))

(deftest record-string-returns-first-non-blank
  (is (= "agent-a" (logic/record-string {:actor-id "agent-a" :session-id "s"} :actor-id :session-id)))
  (is (= "s" (logic/record-string {:actor-id "   " :session-id "s"} :actor-id :session-id))
      "blank strings are skipped")
  (is (= " a " (logic/record-string {:actor-id " a "} :actor-id))
      "returned value is not trimmed (TS parity)")
  (is (nil? (logic/record-string {:actor-id 42} :actor-id)))
  (is (nil? (logic/record-string {} :actor-id))))

(deftest status-tone-maps-statuses
  (is (str/includes? (logic/status-tone "pending") "amber"))
  (is (str/includes? (logic/status-tone "failed") "red"))
  (is (str/includes? (logic/status-tone "delivered") "emerald"))
  (is (str/includes? (logic/status-tone "acknowledged") "sky"))
  (is (str/includes? (logic/status-tone "anything-else") "slate")))

(deftest format-date-contract
  (is (= "—" (logic/format-date nil)))
  (is (= "—" (logic/format-date "")))
  (is (= "not-a-date" (logic/format-date "not-a-date"))
      "unparseable values pass through")
  (is (str/includes? (logic/format-date "2026-01-05T10:00:00Z") "2026")
      "valid dates localize"))

(deftest find-string-deep-contract
  (is (= "r1" (logic/find-string-deep {:run-id "r1"} [:run-id])))
  (is (= "r1" (logic/find-string-deep {:other {:runId "r1"}} [:run-id :runId]))
      "descends into nested maps")
  (is (= "r1" (logic/find-string-deep [{:x 1} {:run-id "r1"}] [:run-id]))
      "descends into vectors")
  (is (= "trimmed" (logic/find-string-deep {:run-id "  trimmed  "} [:run-id]))
      "found values are trimmed")
  (is (= "direct" (logic/find-string-deep {:run-id "direct" :nested {:run-id "deep"}} [:run-id]))
      "direct keys win over nested")
  (is (nil? (logic/find-string-deep {:run-id "   "} [:run-id]))
      "blank strings don't match")
  (is (= "deep" (logic/find-string-deep
                 {:a {:b {:c {:d {:run-id "deep"}}}}} [:run-id]))
      "found at depth 4")
  (is (nil? (logic/find-string-deep
             {:a {:b {:c {:d {:e {:run-id "too-deep"}}}}}} [:run-id]))
      "depth 5 is beyond the limit")
  (is (nil? (logic/find-string-deep "string" [:run-id])))
  (is (nil? (logic/find-string-deep nil [:run-id]))))

(deftest mailbox-links-contract
  (testing "run link from contentRef"
    (let [links (logic/mailbox-links {:contentRef {:run-id "r 1"}})]
      (is (= 1 (count links)))
      (is (= "Open run" (:label (first links))))
      (is (= "/agents?tab=audit&run=r%201" (:path (first links))) "URL-encoded")
      (is (= "r 1" (:detail (first links))))))
  (testing "conversation-id beats session-id for the session link"
    (let [links (logic/mailbox-links {:contentRef {:conversation-id "c1"}
                                      :target {:session-id "s1"}})]
      (is (= ["Open session"] (mapv :label links)))
      (is (= "/agents?tab=audit&session=c1" (:path (first links))))))
  (testing "session-id found in target or source when contentRef lacks it"
    (let [links (logic/mailbox-links {:source {:sessionId "s2"}})]
      (is (= "/agents?tab=audit&session=s2" (:path (first links))))))
  (testing "event link"
    (let [links (logic/mailbox-links {:contentRef {:event-id "e1"}})]
      (is (= ["Open event"] (mapv :label links)))
      (is (= "/events?eventId=e1" (:path (first links))))))
  (testing "all together, in run/session/event order"
    (is (= ["Open run" "Open session" "Open event"]
           (mapv :label (logic/mailbox-links
                         {:contentRef {:runId "r" :session_id "s" :event_id "e"}})))))
  (testing "no ids, no links"
    (is (= [] (logic/mailbox-links {:contentRef {} :source {} :target {}})))))

(deftest unread-count-counts-unacknowledged
  (is (= 2 (logic/unread-count [{:status "pending"} {:status "failed"} {:status "acknowledged"}])))
  (is (= 0 (logic/unread-count []))))

(deftest normalize-entry-contract
  (is (nil? (logic/normalize-entry nil)))
  (is (nil? (logic/normalize-entry {:status "pending"})) "entries without id are dropped")
  (let [entry (logic/normalize-entry {:id "m1"})]
    (is (= "m1" (:id entry)))
    (is (= "actor-message" (:kind entry)) "kind defaults")
    (is (= "pending" (:status entry)) "status defaults")
    (is (= {} (:source entry)) "records default to empty maps")
    (is (= {} (:delivery entry))))
  (let [entry (logic/normalize-entry {:id "m2" :kind "handoff" :status "delivered"
                                      :preview "hi" :delivery {:mode "queue" :attempts 3}})]
    (is (= "handoff" (:kind entry)))
    (is (= "hi" (:preview entry)))
    (is (= {:mode "queue" :attempts 3} (:delivery entry)))))

(deftest normalize-list-response-contract
  (let [resp (logic/normalize-list-response
              {:ok true :box "outbox" :actorId "a1"
               :entries [{:id "m1"} {:no-id true} nil]}
              "inbox")]
    (is (true? (:ok resp)))
    (is (= "outbox" (:box resp)))
    (is (= "a1" (:actor-id resp)))
    (is (= ["m1"] (mapv :id (:entries resp))) "invalid entries dropped"))
  (let [resp (logic/normalize-list-response {} "inbox")]
    (is (true? (:ok resp)) "ok defaults to true")
    (is (= "inbox" (:box resp)) "box falls back")
    (is (= [] (:entries resp))))
  (is (= "inbox" (:box (logic/normalize-list-response {:box "bogus"} "inbox")))
      "unknown box values fall back"))
