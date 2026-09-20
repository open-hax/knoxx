(ns knoxx.backend.infra.agent.session-authority-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.extension-runtime :as extensions]
            [knoxx.backend.infra.agent.session :as session]
            [knoxx.backend.infra.agent.tool-catalog :as catalog]
            [knoxx.backend.shape.agent :as agent]))

(def context
  {:identity/authentication-id "credential-a" :axxium-principal {:principal/id "principal-a"}
   :org-id "org-a" :user-id "user-a" :membership-id "member-a"
   :permissions ["agent.chat.use"]
   :tool-policies [{:tool-id "actors.send-message" :effect "allow"}]})

(deftest ^:async cached-session-cannot-reuse-another-credential-or-policy-closure
  (let [created (atom 0) conversation "session-authority-test"
        previous @session/sessions*]
    (try
      (with-redefs [session/create-session-manager!
                    (fn ([_runtime _config _conversation _model] {:fixture-handle (swap! created inc)})
                      ([_runtime _config _conversation _model _context _thinking _session _spec]
                       {:fixture-handle (swap! created inc)}))
                    catalog/visible-session-signature (fn [_runtime _config _context _spec] "same-visible-tools")
                    extensions/build-extension-ctx (fn [_runtime _config & _args] {})
                    extensions/dispatch-event (fn [_name _event _context] nil)
                    agent/set-thinking-level! (fn [_session _level] nil)]
        (let [ensure! #(session/ensure-agent-session! {} {} conversation "model" %)
              original (await (ensure! context))]
          (is (= original (await (ensure! context))))
          (is (= 1 @created))
          (let [next-login (assoc context :identity/authentication-id "credential-b")
                next-principal (assoc-in next-login [:axxium-principal :principal/id] "principal-b")
                denied (assoc next-principal :tool-policies [{:tool-id "actors.send-message" :effect "deny"}])]
            (is (not= original (await (ensure! next-login))))
            (is (= 2 @created))
            (await (ensure! next-principal))
            (is (= 3 @created))
            (let [denied-session (await (ensure! denied))]
              (is (= 4 @created))
              (is (= denied-session (await (ensure! denied))))
              (is (= 4 @created))))))
      (catch :default error (is false (str "Unexpected session cache failure: " error)))
      (finally (reset! session/sessions* previous)))))
