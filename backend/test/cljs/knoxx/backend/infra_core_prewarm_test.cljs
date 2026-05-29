(ns knoxx.backend.infra-core-prewarm-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.agent.session :as agent-session]
            [knoxx.backend.infra.core :as infra-core]))

(deftest prewarm-sdk-runtime-is-defined
  ;; Regression: prewarm-sdk-runtime! was dropped during namespace renaming but
  ;; the call site in register-app-routes! was not updated, producing
  ;; "Cannot read properties of undefined (reading 'cljs$core$IFn$_invoke$arity$3')"
  ;; at startup. This test ensures the var exists and is callable.
  (testing "prewarm-sdk-runtime! exists and is a function"
    (is (fn? infra-core/prewarm-sdk-runtime!))))

(deftest ^:async prewarm-sdk-runtime-resolves-when-ensure-runtime-succeeds
  (testing "returns Promise<nil> when SDK runtime init succeeds"
    (with-redefs [agent-session/ensure-eta-mu-runtime!
                  (fn [_runtime _config] (js/Promise.resolve {:mock true}))]
      (let [mock-app #js {:log #js {:info (fn [& _]) :error (fn [& _])}}]
        (await (infra-core/prewarm-sdk-runtime! #js {} mock-app {}))
        (is true "prewarm resolved without error")))))

(deftest ^:async prewarm-sdk-runtime-propagates-rejection
  (testing "rejects when SDK runtime init fails"
    (with-redefs [agent-session/ensure-eta-mu-runtime!
                  (fn [_runtime _config] (js/Promise.reject (js/Error. "sdk init failed")))]
      (let [mock-app #js {:log #js {:info (fn [& _]) :error (fn [& _])}}]
        (try
          (await (infra-core/prewarm-sdk-runtime! #js {} mock-app {}))
          (is false "should have rejected")
          (catch :default _err
            (is true "correctly propagated rejection")))))))
