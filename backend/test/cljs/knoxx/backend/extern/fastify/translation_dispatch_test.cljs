(ns knoxx.backend.extern.fastify.translation-dispatch-test
  "Decoding one dispatch request body.

  The body has a single optional field, and that combination is what makes the
  decode worth its own tests: every way of getting it wrong turns a scoped
  request into a whole-corpus sweep, which is the most expensive thing this
  route can do."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.fastify.translation-dispatch :as adapter]))

(defn- request
  "A request whose body is `body`. Only `.body` is read by `decode-request`."
  [body]
  (js-obj "body" (clj->js body)))

(defn- request-with-raw-body
  "A request whose body is passed through unconverted, so a CLJS map survives.

  `fastify/request-body` returns a CLJS map unchanged, which is how a nil value
  can be distinguished from an absent key — `clj->js` would erase that."
  [body]
  (js-obj "body" body))

(deftest an-omitted-document-means-the-whole-corpus
  (testing "an empty body is the ordinary operator sweep"
    (is (= {} (adapter/decode-request (request {}))))))

(deftest a-named-document-is-decoded-to-its-qualified-identity
  (testing "a wire string becomes the keyword the resource index is keyed by"
    (is (= {:document :knoxx.docs/probe}
           (adapter/decode-request (request {:document "knoxx.docs/probe"}))))))

(deftest a-present-but-empty-document-is-refused
  ;; The regression: dropping a blank document from the decoded map turned
  ;; {"document": ""} into a request to translate everything.
  (testing "an empty string is not a way of saying all documents"
    (is (thrown? js/Error (adapter/decode-request (request {:document ""})))))

  (testing "a blank string is refused too"
    (is (thrown? js/Error (adapter/decode-request (request {:document "   "})))))

  (testing "an explicit nil is refused"
    (is (thrown? js/Error
                 (adapter/decode-request
                  (request-with-raw-body {:document nil}))))))

(deftest an-unqualified-document-is-refused
  (testing "a bare name is a different document from the qualified one"
    ;; Accepted, it would sweep nothing while reporting success.
    (is (thrown? js/Error (adapter/decode-request (request {:document "probe"}))))))

(deftest an-unrecognized-field-is-refused
  (testing "a typo must not be reinterpreted as a whole-corpus sweep"
    (is (thrown? js/Error
                 (adapter/decode-request (request {:documnet "knoxx.docs/probe"}))))))
