(ns knoxx.backend.infra-core-prewarm-test
  (:require [cljs.test :refer [async deftest is testing]]
            [knoxx.backend.infra.agent.session :as agent-session]
            [knoxx.backend.infra.core :as infra-core]))

(deftest prewarm-sdk-runtime-is-defined
  ;; Regression: prewarm-sdk-runtime! was dropped during namespace renaming but
  ;; the call site in register-app-routes! was not updated, producing
  ;; "Cannot read properties of undefined (reading 'cljs$core$IFn$_invoke$arity$3')"
  ;; at startup. This test ensures the var exists and is callable.
  (testing "prewarm-sdk-runtime! exists and is a function"
    (is (fn? infra-core/prewarm-sdk-runtime!))))

(deftest prewarm-sdk-runtime-resolves-when-ensure-runtime-succeeds
  (async done
    (testing "returns Promise<nil> when SDK runtime init succeeds"
      (with-redefs [agent-session/ensure-eta-mu-runtime!
                    (fn [_runtime _config] (js/Promise.resolve {:mock true}))]
        (let [mock-app #js {:log #js {:info (fn [& _]) :error (fn [& _])}}]
          (-> (infra-core/prewarm-sdk-runtime! #js {} mock-app {})
              (.then (fn [_]
                       (is true "prewarm resolved without error")
                       (done)))
              (.catch (fn [err]
                        (is false (str "prewarm-sdk-runtime! rejected: " err))
                        (done)))))))))

(deftest prewarm-sdk-runtime-propagates-rejection
  (async done
    (testing "rejects when SDK runtime init fails"
      (with-redefs [agent-session/ensure-eta-mu-runtime!
                    (fn [_runtime _config] (js/Promise.reject (js/Error. "sdk init failed")))]
        (let [mock-app #js {:log #js {:info (fn [& _]) :error (fn [& _])}}]
          (-> (infra-core/prewarm-sdk-runtime! #js {} mock-app {})
              (.then (fn [_]
                       (is false "should have rejected")
                       (done)))
              (.catch (fn [_err]
                        (is true "correctly propagated rejection")
                        (done)))))))))
