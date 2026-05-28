(ns knoxx.backend.pipeline-runner-test
  (:require [clojure.test :refer [deftest is testing]]
            [knoxx.backend.infra.temp-memory :as temp]
            [knoxx.backend.shape.pipeline :as pipeline-shape]
            [promesa.core :as p]))

;; ── dependency-order ─────────────────────────────

(deftest dependency-order-simple
  (testing "steps with no deps stay in order"
    (let [steps [{:step/id "a" :step/contract "x"}
                 {:step/id "b" :step/contract "y"}]]
      (is (= ["a" "b"] (->> (pipeline-shape/dependency-order steps) (map :step/id))))))
  (testing "steps with deps are sorted"
    (let [steps [{:step/id "b" :step/contract "y" :step/depends-on ["a"]}
                 {:step/id "a" :step/contract "x"}]]
      (is (= ["a" "b"] (->> (pipeline-shape/dependency-order steps) (map :step/id)))))))

;; ── interpolation ─────────────────────────────

(deftest interpolate-value-no-placeholder
  (testing "string without placeholder passes through"
    (p/let [result (pipeline-shape/interpolate-value "hello" {:k "v"})]
      (is (= "hello" result)))))

(deftest interpolate-value-with-placeholder
  (testing "{{memory.temp:key}} is replaced"
    (p/let [_ (temp/mem-set! "mykey" "world" {:ttl 60})
            result (pipeline-shape/interpolate-value "hello {{memory.temp:mykey}}!" {"mykey" "world"})]
      (is (= "hello world!" result)))))

(deftest interpolate-map-nested
  (testing "nested map interpolation"
    (p/let [_ (temp/mem-set! "name" "Knoxx" {:ttl 60})
            result (pipeline-shape/interpolate-map
                    {:greeting "Hello {{memory.temp:name}}"
                     :nested {:msg "Hi {{memory.temp:name}}"}}
                    {"name" "Knoxx"})]
      (is (= "Hello Knoxx" (:greeting result)))
      (is (= "Hi Knoxx" (get-in result [:nested :msg]))))))
