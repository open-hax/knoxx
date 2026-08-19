(ns knoxx.backend.extern.fastify-cms-publication-test
  "Status and body classification for the CMS publication adapter.

  This adapter had no test at all, which is how a 403 could be reported as a 500
  without anything noticing. Every case goes through the public `respond!`,
  because that is where the failing operation actually lands: `guarded!` runs
  inside it, so an authorization denial is caught here rather than escaping to
  Fastify."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.fastify.cms-publication :as adapter]))

(defn- capturing
  "Handlers whose `json-response!` records what the adapter decided."
  [sent]
  {:json-response! (fn [_reply status body]
                     (reset! sent {:status status :body body})
                     nil)})

(defn- ^:async status-for
  "The status this adapter assigns to an operation that throws `err`."
  [err]
  (let [sent (atom nil)]
    (await (adapter/respond! (capturing sent) #js {} (fn [] (throw err))))
    @sent))

;; ── A carried status wins ──────────────────────────────────────────────────

(deftest ^:async a-denied-request-is-a-403-not-a-500
  (testing "ensure-permission! throws http-error with :status in ex-data, and
            guarded! runs inside respond! — so the denial is classified here.
            Without reading the carried status it fell through to 500, telling
            the caller to retry a request that will never succeed"
    (let [sent (await (status-for (ex-info "forbidden" {:status 403
                                                       :permission "org.publications.manage"})))]
      (is (= 403 (:status sent))))))

(deftest ^:async a-status-on-the-js-error-object-is-read-too
  (testing "the real http-error carries statusCode as a JS property; a fake
            permission check in a test throws plain ex-info. Both must work"
    (let [err (js/Error. "forbidden")]
      (aset err "statusCode" 403)
      (is (= 403 (:status (await (status-for err))))))))

(deftest ^:async a-carried-status-beats-the-shape-guesses
  (testing "a denial whose ex-data also happens to name a publication must not
            be reported as 404 — the resource exists, the caller may not have it"
    (let [sent (await (status-for (ex-info "forbidden"
                                           {:status 403
                                            :publication/id :knoxx.docs/probe-es})))]
      (is (= 403 (:status sent))))))

;; ── Shape-based classification, when nothing is carried ────────────────────

(deftest ^:async resource-shaped-failures-keep-their-statuses
  (testing "an unknown publication is a 404"
    (is (= 404 (:status (await (status-for
                                (ex-info "unknown publication"
                                         {:publication/id :knoxx.docs/probe-es})))))))
  (testing "an unknown document is a 404"
    (is (= 404 (:status (await (status-for
                                (ex-info "unknown document"
                                         {:document/id :knoxx.docs/probe})))))))
  (testing "a conflict is a 409 — a retry will not help, the data contradicts"
    (is (= 409 (:status (await (status-for
                                (ex-info "conflicting entries"
                                         {:conflicts [{:matches 2}]})))))))
  (testing "blockers are a 409 for the same reason"
    (is (= 409 (:status (await (status-for
                                (ex-info "unresolved references"
                                         {:blockers [:unresolved-document]})))))))
  (testing "a rejected patch body is a 422"
    (is (= 422 (:status (await (status-for
                                (ex-info "invalid patch" {:errors {:state ["unknown"]}})))))))
  (testing "and anything unrecognized stays a 500 rather than guessing"
    (is (= 500 (:status (await (status-for (ex-info "boom" {}))))))))

;; ── Body shape ─────────────────────────────────────────────────────────────

(deftest ^:async the-error-body-is-the-one-contract-shape
  (testing "knoxx.backend.law.error-body puts the message in :detail and the
            ex-data in :error, on every route of this surface"
    ;; The body reaching json-response! is CLJS data — clj->js happens inside
    ;; send-json!, past this seam.
    (let [sent (await (status-for (ex-info "conflicting entries"
                                           {:conflicts [{:matches 2}]})))
          body (:body sent)]
      (is (= "conflicting entries" (:detail body)))
      (is (some? (:error body))))))

(deftest ^:async a-successful-operation-is-a-200
  (let [sent (atom nil)]
    (await (adapter/respond! (capturing sent) #js {}
                             (fn [] {:publication/id :knoxx.docs/probe-es})))
    (is (= 200 (:status @sent)))
    (testing "and identity crosses the wire qualified, not collapsed onto its name
              — clj->js renders a keyword with `name`, which would have sent
              \"probe-es\" and merged every namespace onto one wire id"
      (is (= "knoxx.docs/probe-es" (:publication/id (:body @sent)))))))
