(ns knoxx.backend.extern-node-env-test
  "Regression coverage for the process-environment conversion.

  Required of a new extern adapter by AGENTS.md: the point of the boundary is
  that a JavaScript value becomes a CLJS scalar, so the conversion itself is
  what gets pinned."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.node-env :as node-env]))

(defn- with-env
  "Set `name` to `value` for the duration of `f`, restoring whatever was there.

   `js-delete` rather than assigning nil: assigning nil to process.env produces
   the STRING \"null\", which is exactly the kind of JavaScript truth this
   adapter exists to keep out of the rest of the codebase."
  [name value f]
  (let [previous (aget js/process.env name)]
    (try
      (if (nil? value)
        (js-delete js/process.env name)
        (aset js/process.env name value))
      (f)
      (finally
        (if (nil? previous)
          (js-delete js/process.env name)
          (aset js/process.env name previous))))))

(deftest variable-returns-a-cljs-scalar-or-nil
  (testing "a set variable comes back as a trimmed string"
    (with-env "KNOXX_TEST_NODE_ENV" "  a-value  "
      #(is (= "a-value" (node-env/variable "KNOXX_TEST_NODE_ENV")))))

  (testing "absent, empty and whitespace-only all collapse to nil"
    ;; The collapse every caller depends on: a deployment writing FOO= means the
    ;; same as one omitting FOO, and a sentinel of spaces is not a secret.
    (with-env "KNOXX_TEST_NODE_ENV" nil
      #(is (nil? (node-env/variable "KNOXX_TEST_NODE_ENV"))))
    (with-env "KNOXX_TEST_NODE_ENV" ""
      #(is (nil? (node-env/variable "KNOXX_TEST_NODE_ENV"))))
    (with-env "KNOXX_TEST_NODE_ENV" "   \t "
      #(is (nil? (node-env/variable "KNOXX_TEST_NODE_ENV")))))

  (testing "a name that was never set is nil rather than an error"
    (is (nil? (node-env/variable "KNOXX_TEST_NAME_THAT_IS_NOT_SET")))))
