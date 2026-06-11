(ns knoxx.backend.shape.parse-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.shape.parse :as parse]))

(deftest parse-positive-int-parses-valid-integers
  (testing "parses positive integers from strings and numbers"
    (is (= 1 (parse/parse-positive-int "1")))
    (is (= 42 (parse/parse-positive-int "42")))
    (is (= 100 (parse/parse-positive-int 100)))
    (is (= 1 (parse/parse-positive-int 1.0)))))

(deftest parse-positive-int-rejects-non-positive
  (testing "rejects zero, negative, and non-numeric values"
    (is (nil? (parse/parse-positive-int "0")))
    (is (nil? (parse/parse-positive-int "-1")))
    (is (nil? (parse/parse-positive-int -5)))
    (is (nil? (parse/parse-positive-int "")))
    (is (nil? (parse/parse-positive-int "abc")))
    (is (nil? (parse/parse-positive-int "3.14")))
    (is (nil? (parse/parse-positive-int nil)))
    (is (nil? (parse/parse-positive-int {})))))

(deftest truthy-param-recognizes-truthy-values
  (testing "recognizes boolean true, positive numbers, and truthy strings"
    (is (true? (parse/truthy-param? true)))
    (is (true? (parse/truthy-param? 1)))
    (is (true? (parse/truthy-param? 42)))
    (is (true? (parse/truthy-param? "1")))
    (is (true? (parse/truthy-param? "true")))
    (is (true? (parse/truthy-param? "yes")))
    (is (true? (parse/truthy-param? "on")))
    (is (true? (parse/truthy-param? "force")))
    (is (true? (parse/truthy-param? "  TRUE  ")))))

(deftest truthy-param-rejects-falsy-values
  (testing "rejects false, zero, negative numbers, and falsy strings"
    (is (false? (parse/truthy-param? false)))
    (is (false? (parse/truthy-param? 0)))
    (is (false? (parse/truthy-param? -1)))
    (is (false? (parse/truthy-param? "")))
    (is (false? (parse/truthy-param? "false")))
    (is (false? (parse/truthy-param? "no")))
    (is (false? (parse/truthy-param? "off")))
    (is (false? (parse/truthy-param? nil)))
    (is (false? (parse/truthy-param? {})))))
