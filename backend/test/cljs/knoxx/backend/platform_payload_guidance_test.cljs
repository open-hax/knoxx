(ns knoxx.backend.platform-payload-guidance-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.social.api-catalog :as discovery]
            [knoxx.backend.domain.social.api-schema :as guide]
            [knoxx.backend.extern.platform-api-catalog :as catalog]
            [knoxx.backend.platform-json-request-test :as fixture]
            [knoxx.backend.law.platform-payload :as payload]))

(def poll
  {"type" "object" "required" ["question" "answers"]
   "properties" {"question" {"type" "object" "required" ["text"] "properties" {"text" {"type" "string"}}}
                 "answers" {"type" "array" "items" {"$ref" "#/components/schemas/Answer"}}
                 "duration" {"type" ["integer" "null"] "minimum" 1 "maximum" 768 "description" "Number of hours"}}})
(def answer
  {"type" "object" "required" ["poll_media"]
   "properties" {"poll_media" {"type" "object" "properties" {"text" {"type" "string"}}}}})
(def asset
  {"documents" {"discord.openapi" {"components" {"schemas" {"Poll" poll "Answer" answer}}}}})
(def schema
  {"type" "object" "properties" {"poll" {"oneOf" [{"type" "null"} {"$ref" "#/components/schemas/Poll"}]}}})

(deftest invalid-poll-feedback-survives-adapters-and-does-not-echo-private-values
  (let [body {"poll" {"answers" [{"text" "private answer value"}] "duration" 86400}}
        error (try (payload/assert-body! asset "discord.openapi" schema body) nil (catch :default e e))
        issues (:issues (ex-data error))]
    (is (= #{["body" "poll" "question"] ["body" "poll" "answers" 0 "poll_media"] ["body" "poll" "duration"]}
           (set (map :path issues))))
    (is (re-find #"Number of hours" (ex-message error)))
    (is (re-find #"poll_media" (ex-message error)))
    (is (not (re-find #"private answer value" (ex-message error))))
    (is (payload/issues-valid? issues))
    (is (= :preflight (:phase (ex-data error))))))

(deftest correctly-shaped-native-poll-and-explicit-null-remain-admissible
  (doseq [body [{"poll" nil} {"poll" {"question" {"text" "Workshop?"} "answers" [{"poll_media" {"text" "Pulse"}}] "duration" 24}}]]
    (is (= body (payload/assert-body! asset "discord.openapi" schema body)))))

(deftest request-guidance-expands-nested-references-and-preserves-units
  (let [view (guide/request-guide asset "discord.openapi" {"requestBody" {"content" {"application/json" {"schema" schema}}}})
        poll-view (get-in view [:schema "properties" "poll" "oneOf" 1])]
    (is (= ["question" "answers"] (get poll-view "required")))
    (is (= ["poll_media"] (get-in poll-view ["properties" "answers" "items" "required"])))
    (is (= "Number of hours" (get-in poll-view ["properties" "duration" "description"])))
    (is (= 768 (get-in poll-view ["properties" "duration" "maximum"])))))

(deftest cyclic-schema-view-terminates-with-local-reference-instead-of-hiding-fields
  (let [cyclic {"documents" {"doc" {"defs" {"main" {"type" "object" "properties" {"next" {"type" "ref" "ref" "#main"}}}}}}}
        view (guide/request-guide cyclic "doc" {"input" {"schema" {"type" "ref" "ref" "#main"}}})]
    (is (true? (get-in view [:schema "properties" "next" "viewLimited"])))
    (is (= "#main" (get-in view [:schema "properties" "next" "reference"])))))

(deftest ^:async actual-message-guide-keeps-native-poll-fields-visible-on-the-model-wire
  (let [asset (await (fixture/pinned-catalog))
        view (discovery/describe asset {:platform "discord" :operation "create_message"})
        rendered (catalog/render view)]
    (is (false? (:truncated rendered)))
    (is (re-find #"poll_media" (:text rendered)))
    (is (re-find #"Number of hours" (:text rendered)))
    (is (re-find #"question" (:text rendered)))
    (is (< (count (:text rendered)) 20000))
    (is (map? (:definition view)) "Complete raw definition remains available in structured details")))
