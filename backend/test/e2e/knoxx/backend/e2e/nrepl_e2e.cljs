(ns knoxx.backend.e2e.nrepl-e2e
  "The nREPL tool, against a real socket speaking real bencode.

   nrepl.eval is the highest-risk tool on the surface — arbitrary evaluation in
   the live runtime — and it had no test. It also hand-rolls the bencode
   protocol, which is exactly the kind of code that works until a message
   arrives split across two TCP chunks.

   The double on the other end is an independent bencode implementation, so a
   framing bug cannot encode and decode symmetrically and pass."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.e2e.harness :as harness]
            [knoxx.backend.e2e.mcp-client :as mcp]
            [knoxx.backend.e2e.nrepl-double :as nrepl-double]))

(defn- request-op
  [requests op]
  (some #(when (= op (get % "op")) %) requests))

(deftest ^:async nrepl-eval-speaks-the-protocol-test
  (let [started (await (harness/start!))]
    (try
      (await
       (nrepl-double/with-nrepl!
         {:eval-value "{:value 2, :ns cljs.user}"}
         (^:async fn [server]
           (let [client  (harness/client started)
                 _       (await (mcp/initialize! client))
                 outcome (mcp/call-outcome
                          (await (mcp/call-tool! client "nrepl_eval"
                                                 {:code "(+ 1 1)"
                                                  :target "cljs"
                                                  :build_id "server"
                                                  :ns "cljs.user"
                                                  :timeout_ms 10000})))
                 requests ((:requests server))]

             (testing "the evaluation completed and returned the value"
               (is (= :ok (:status outcome))
                   (str "nrepl_eval failed: " (:detail outcome)))
               (is (str/includes? (str (:detail outcome)) "2")
                   (str "the value the nREPL returned did not reach the caller: "
                        (:detail outcome))))

             (testing "it opened a session before evaluating"
               (is (some? (request-op requests "clone"))
                   (str "no clone op was sent: " (pr-str requests)))
               (is (= "clone" (get (first requests) "op"))
                   "clone must come first; an eval without a session is refused by nREPL"))

             (testing "the eval carried the session the clone established"
               (let [eval-request (request-op requests "eval")]
                 (is (some? eval-request)
                     (str "no eval op was sent: " (pr-str requests)))
                 (is (= "e2e-nrepl-session" (get eval-request "session"))
                     (str "the eval did not reuse the cloned session: "
                          (pr-str eval-request)))))

             (testing "cljs evaluation is forwarded through shadow's cljs-eval"
               (let [code (get (request-op requests "eval") "code")]
                 (is (str/includes? (str code) "shadow.cljs.devtools.api")
                     (str "target=cljs must route through shadow's api, got: " code))
                 (is (str/includes? (str code) ":server")
                     (str "the build id did not reach the eval form: " code))
                 (is (str/includes? (str code) "cljs.user")
                     (str "the namespace did not reach the eval form: " code))
                 (is (str/includes? (str code) "(+ 1 1)")
                     (str "the caller's code did not reach the eval form: " code))))))))
      (finally (await (harness/stop! started))))))

(deftest ^:async nrepl-eval-targets-the-jvm-directly-test
  (let [started (await (harness/start!))]
    (try
      (await
       (nrepl-double/with-nrepl!
         {:eval-value "4"}
         (^:async fn [server]
           (let [client  (harness/client started)
                 _       (await (mcp/initialize! client))
                 outcome (mcp/call-outcome
                          (await (mcp/call-tool! client "nrepl_eval"
                                                 {:code "(* 2 2)" :target "clj"})))
                 code    (get (request-op ((:requests server)) "eval") "code")]
             (is (= :ok (:status outcome))
                 (str "nrepl_eval target=clj failed: " (:detail outcome)))
             (is (= "(* 2 2)" (str code))
                 (str "target=clj must evaluate the caller's code verbatim on the "
                      "JVM, not wrap it in shadow's cljs-eval; got: " code))))))
      (finally (await (harness/stop! started))))))

(deftest ^:async nrepl-eval-reports-an-unreachable-server-test
  ;; No double is started, so the configured port has nothing listening. The
  ;; tool must say so rather than hang until its timeout, and must not fail at
  ;; the protocol level — a connection refusal is the tool's answer to give.
  (let [started (await (harness/start!))
        previous-port (aget js/process.env "KNOXX_NREPL_PORT")]
    (try
      ;; Port 1 is privileged and never bound by a user process.
      (aset js/process.env "KNOXX_NREPL_PORT" "1")
      (let [client  (harness/client started)
            _       (await (mcp/initialize! client))
            outcome (mcp/call-outcome
                     (await (mcp/call-tool! client "nrepl_eval"
                                            {:code "(+ 1 1)" :timeout_ms 5000})))]
        (is (= :tool-error (:status outcome))
            (str "an unreachable nREPL must be reported as a tool error, got "
                 (pr-str outcome))))
      (finally
        (if previous-port
          (aset js/process.env "KNOXX_NREPL_PORT" previous-port)
          (js-delete js/process.env "KNOXX_NREPL_PORT"))
        (await (harness/stop! started))))))
