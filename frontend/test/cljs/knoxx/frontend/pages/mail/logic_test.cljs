(ns knoxx.frontend.pages.mail.logic-test
  "Written FIRST (TDD) — pure-logic contract for the Helix port of
  src/pages/MailPage.tsx (record-string, status-tone, format-date,
  find-string-deep, mailbox-links, unread-count) plus the mailbox
  normalizers from src/lib/api/runtime.ts."
  (:require [cljs.test :as t]
            [clojure.string :as str]
            [knoxx.frontend.pages.mail.logic :as logic]))

(t/deftest record-string-returns-first-non-blank
  (t/is (= "agent-a" (logic/record-string {:actor-id "agent-a" :session-id "s"} :actor-id :session-id)))
  (t/is (= "s" (logic/record-string {:actor-id "   " :session-id "s"} :actor-id :session-id))
      "blank strings are skipped")
  (t/is (= " a " (logic/record-string {:actor-id " a "} :actor-id))
      "returned value is not trimmed (TS parity)")
  (t/is (nil? (logic/record-string {:actor-id 42} :actor-id)))
  (t/is (nil? (logic/record-string {} :actor-id))))

(t/deftest status-tone-maps-statuses
  (t/is (str/includes? (logic/status-tone "pending") "amber"))
  (t/is (str/includes? (logic/status-tone "failed") "red"))
  (t/is (str/includes? (logic/status-tone "delivered") "emerald"))
  (t/is (str/includes? (logic/status-tone "acknowledged") "sky"))
  (t/is (str/includes? (logic/status-tone "anything-else") "slate")))

(t/deftest format-date-contract
  (t/is (= "—" (logic/format-date nil)))
  (t/is (= "—" (logic/format-date "")))
  (t/is (= "not-a-date" (logic/format-date "not-a-date"))
      "unparseable values pass through")
  (t/is (str/includes? (logic/format-date "2026-01-05T10:00:00Z") "2026")
      "valid dates localize"))

(t/deftest find-string-deep-contract
  (t/is (= "r1" (logic/find-string-deep {:run-id "r1"} [:run-id])))
  (t/is (= "r1" (logic/find-string-deep {:other {:runId "r1"}} [:run-id :runId]))
      "descends into nested maps")
  (t/is (= "r1" (logic/find-string-deep [{:x 1} {:run-id "r1"}] [:run-id]))
      "descends into vectors")
  (t/is (= "trimmed" (logic/find-string-deep {:run-id "  trimmed  "} [:run-id]))
      "found values are trimmed")
  (t/is (= "direct" (logic/find-string-deep {:run-id "direct" :nested {:run-id "deep"}} [:run-id]))
      "direct keys win over nested")
  (t/is (nil? (logic/find-string-deep {:run-id "   "} [:run-id]))
      "blank strings don't match")
  (t/is (= "deep" (logic/find-string-deep
                 {:a {:b {:c {:d {:run-id "deep"}}}}} [:run-id]))
      "found at depth 4")
  (t/is (nil? (logic/find-string-deep
             {:a {:b {:c {:d {:e {:run-id "too-deep"}}}}}} [:run-id]))
      "depth 5 is beyond the limit")
  (t/is (nil? (logic/find-string-deep "string" [:run-id])))
  (t/is (nil? (logic/find-string-deep nil [:run-id]))))

(t/deftest mailbox-links-contract
  (t/testing "run link from contentRef"
    (let [links (logic/mailbox-links {:contentRef {:run-id "r 1"}})]
      (t/is (= 1 (count links)))
      (t/is (= "Open run" (:label (first links))))
      (t/is (= "/agents?tab=audit&run=r%201" (:path (first links))) "URL-encoded")
      (t/is (= "r 1" (:detail (first links))))))
  (t/testing "conversation-id beats session-id for the session link"
    (let [links (logic/mailbox-links {:contentRef {:conversation-id "c1"}
                                      :target {:session-id "s1"}})]
      (t/is (= ["Open session"] (mapv :label links)))
      (t/is (= "/agents?tab=audit&session=c1" (:path (first links))))))
  (t/testing "session-id found in target or source when contentRef lacks it"
    (let [links (logic/mailbox-links {:source {:sessionId "s2"}})]
      (t/is (= "/agents?tab=audit&session=s2" (:path (first links))))))
  (t/testing "event link"
    (let [links (logic/mailbox-links {:contentRef {:event-id "e1"}})]
      (t/is (= ["Open event"] (mapv :label links)))
      (t/is (= "/events?eventId=e1" (:path (first links))))))
  (t/testing "all together, in run/session/event order"
    (t/is (= ["Open run" "Open session" "Open event"]
           (mapv :label (logic/mailbox-links
                         {:contentRef {:runId "r" :session_id "s" :event_id "e"}})))))
  (t/testing "no ids, no links"
    (t/is (= [] (logic/mailbox-links {:contentRef {} :source {} :target {}})))))

(t/deftest unread-count-counts-unacknowledged
  (t/is (= 2 (logic/unread-count [{:status "pending"} {:status "failed"} {:status "acknowledged"}])))
  (t/is (= 0 (logic/unread-count []))))

(t/deftest normalize-entry-contract
  (t/is (nil? (logic/normalize-entry nil)))
  (t/is (nil? (logic/normalize-entry {:status "pending"})) "entries without id are dropped")
  (let [entry (logic/normalize-entry {:id "m1"})]
    (t/is (= "m1" (:id entry)))
    (t/is (= "actor-message" (:kind entry)) "kind defaults")
    (t/is (= "pending" (:status entry)) "status defaults")
    (t/is (= {} (:source entry)) "records default to empty maps")
    (t/is (= {} (:delivery entry))))
  (let [entry (logic/normalize-entry {:id "m2" :kind "handoff" :status "delivered"
                                      :preview "hi" :delivery {:mode "queue" :attempts 3}})]
    (t/is (= "handoff" (:kind entry)))
    (t/is (= "hi" (:preview entry)))
    (t/is (= {:mode "queue" :attempts 3} (:delivery entry)))))

(t/deftest normalize-list-response-contract
  (let [resp (logic/normalize-list-response
              {:ok true :box "outbox" :actorId "a1"
               :entries [{:id "m1"} {:no-id true} nil]}
              "inbox")]
    (t/is (true? (:ok resp)))
    (t/is (= "outbox" (:box resp)))
    (t/is (= "a1" (:actor-id resp)))
    (t/is (= ["m1"] (mapv :id (:entries resp))) "invalid entries dropped"))
  (let [resp (logic/normalize-list-response {} "inbox")]
    (t/is (true? (:ok resp)) "ok defaults to true")
    (t/is (= "inbox" (:box resp)) "box falls back")
    (t/is (= [] (:entries resp))))
  (t/is (= "inbox" (:box (logic/normalize-list-response {:box "bogus"} "inbox")))
      "unknown box values fall back"))
