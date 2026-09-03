(ns knoxx.frontend.lib.publication-wire-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.frontend.lib.publication-wire :as pw]
            [open-hax.publication-wire :as wire]))

;; ── fetch stub ─────────────────────────────────────────────────────────────
;;
;; The review thread asked for this specifically: do NOT hand-roll `clj->js` /
;; `js->clj` in the test. Drive the real helper against a stubbed `js/fetch` and
;; inspect what actually went over the wire, so the assertion breaks if
;; `api/request`'s serialization ever changes.

(defn- ^:async with-fetch-stub
  "Replace js/fetch with a recorder, run `body`, restore. Returns a promise of
   `{:calls [...] :result r}`."
  [response body]
  (let [original js/fetch
        calls (atom [])]
    (set! js/fetch
          (fn [path init]
            (swap! calls conj {:path path
                               :method (some-> init .-method)
                               :raw-body (some-> init .-body)})
            (js/Promise.resolve
             #js {:ok true
                  :status 200
                  :json (fn [] (js/Promise.resolve (clj->js response)))
                  :text (fn [] (js/Promise.resolve ""))})))
    (try
      (let [result (await (body))]
        {:calls @calls :result result})
      (finally
        (set! js/fetch original)))))

(defn- sent-body
  "The parsed JSON body of the single recorded call, keywordized exactly the way
   the backend's HTTP layer would see it."
  [calls]
  (-> (:raw-body (first calls))
      js/JSON.parse
      (js->clj :keywordize-keys true)))

;; ── Wire fixtures ──────────────────────────────────────────────────────────

(def list-response
  {:documents [{:document {:id "knoxx.docs/probe"
                           :title "Probe"
                           :source-locale "en"
                           :source {:path "docs/probe.md"}}
                :publications [{:id "knoxx.docs/probe-es"
                                :document "knoxx.docs/probe"
                                :garden "knoxx.docs/promethean"
                                :locale "es"
                                :revision "source/current"
                                :path "/probe"
                                :desired "published"
                                :observed nil
                                :blockers ["translation-missing"]}
                               {:id "knoxx.docs/probe-fr"
                                :document "knoxx.docs/probe"
                                :garden "knoxx.docs/legacy"
                                :locale "fr"
                                :revision "abc123"
                                :path "/probe-fr"
                                :desired "withheld"
                                :observed "abc123"
                                :blockers []}]}]
   :gardens [{:id "knoxx.docs/promethean" :title "Promethean" :status "active"}
             {:id "knoxx.docs/legacy" :title "Legacy" :status "archived"}]})

;; ── The state-patch regression ─────────────────────────────────────────────

(deftest ^:async publish-request-body-matches-the-shared-contract
  (testing "the body is built from the same vocabulary the backend contract is
            built from, and it survives api/request's own serialization"
    (let [{:keys [calls]} (await (with-fetch-stub
                                   {}
                                   #(pw/publish! :knoxx.docs/probe-es)))
          body (sent-body calls)]
      (is (= 1 (count calls)))
      (is (= "PATCH" (:method (first calls))))
      (testing "the wire key is unqualified — a namespace would not survive clj->js"
        (is (= [wire/state-patch-key] (keys body)))
        (is (= :state wire/state-patch-key))
        (is (not (contains? body :publication/state))))
      (testing "and the value is the shared enum string"
        (is (= (wire/encode-state :published) (get body wire/state-patch-key)))
        (is (= "published" (get body wire/state-patch-key))))
      (testing "which decodes back to the canonical domain state"
        (is (= :published (wire/decode-state (get body wire/state-patch-key))))))))

(deftest ^:async every-state-edit-sends-a-contract-valid-body
  (doseq [[helper expected] [[pw/publish! :published]
                             [pw/unpublish! :withheld]
                             [pw/archive! :archived]]]
    (testing (str expected)
      (let [{:keys [calls]} (await (with-fetch-stub {} #(helper :knoxx.docs/probe-es)))
            body (sent-body calls)]
        (is (= (wire/encode-state expected) (get body wire/state-patch-key)))
        (is (contains? (set wire/state-wire-values)
                       (get body wire/state-patch-key)))))))

(deftest ^:async draft-round-trips-through-cms-get-and-state-patch
  (let [draft-response (assoc-in list-response
                                 [:documents 0 :publications 0 :desired]
                                 "draft")
        {:keys [result]} (await (with-fetch-stub draft-response #(pw/load-cms!)))]
    (testing "a draft row returned by CMS GET is decoded as desired state"
      (is (= :draft (get-in result [:documents 0 :publications 0 :desired])))))
  (let [{:keys [calls]} (await (with-fetch-stub
                                {}
                                #(pw/set-publication-state!
                                  :knoxx.docs/probe-es
                                  :draft)))
        body (sent-body calls)]
    (testing "the generic state PATCH sends the shared draft spelling"
      (is (= {:state "draft"} body))
      (is (= :draft (wire/decode-state (:state body)))))))

(deftest ^:async patch-url-carries-no-encoded-colon
  (let [{:keys [calls]} (await (with-fetch-stub {} #(pw/publish! :knoxx.docs/probe-es)))
        path (:path (first calls))]
    (is (str/includes? path "knoxx.docs"))
    (testing "a qualified id encodes without an EDN leading colon, so no %3A"
      (is (not (str/includes? path "%3A")))
      (is (not (str/includes? path ":"))))))

;; ── Identity round trip ────────────────────────────────────────────────────

(deftest resource-id-round-trip
  (doseq [id [:docs/probe :knoxx.docs/translation-pipeline :bare]]
    (testing (str id)
      (is (= id (pw/decode-id (pw/encode-id id))))
      (is (not (str/starts-with? (pw/encode-id id) ":")))))
  (testing "docs/probe encodes exactly, with no colon"
    (is (= "docs/probe" (pw/encode-id :docs/probe)))
    (is (= :docs/probe (pw/decode-id "docs/probe"))))
  (testing "namespaces do not collapse"
    (is (not= (pw/encode-id :tenant-a/foo) (pw/encode-id :tenant-b/foo)))))

;; ── Row decoding ───────────────────────────────────────────────────────────

(deftest ^:async load-cms!-populates-from-a-normalized-response
  (let [{:keys [result]} (await (with-fetch-stub list-response #(pw/load-cms!)))]
    (testing "the response is not double-wrapped"
      (is (= #{:documents :gardens} (set (keys result))))
      (is (= #{:document :publications} (set (keys (first (:documents result)))))))
    (let [document (get-in result [:documents 0 :document])
          [spanish french] (get-in result [:documents 0 :publications])
          [active archived] (:gardens result)]
      (testing "document row"
        (is (= :knoxx.docs/probe (:id document)))
        (is (= :en (:source-locale document)))
        (is (= "docs/probe.md" (get-in document [:source :path]))))
      (testing "garden rows decode status to keywords"
        (is (= :knoxx.docs/promethean (:id active)))
        (is (= :active (:status active)))
        (is (= :archived (:status archived))))
      (testing "publication rows decode every keyword-valued field"
        (is (= :knoxx.docs/probe-es (:id spanish)))
        (is (= :knoxx.docs/probe (:document spanish)))
        (is (= :knoxx.docs/promethean (:garden spanish)))
        (is (= :es (:locale spanish)))
        (is (= :published (:desired spanish)))
        (is (= [:translation-missing] (:blockers spanish))))
      (testing "a selector revision decodes to a keyword, a concrete one stays a string"
        (is (= :source/current (:revision spanish)))
        (is (= "abc123" (:revision french)))
        (is (string? (:revision french))))
      (testing "observed is runtime evidence and stays as-is"
        (is (nil? (:observed spanish)))
        (is (= "abc123" (:observed french)))))))

(deftest ^:async loader-uses-native-async-not-promise-chains
  (testing "load-cms! awaits rather than chaining"
    (let [{:keys [result]} (await (with-fetch-stub list-response #(pw/load-cms!)))]
      (is (map? result) "a .then chain would have resolved to a promise here")
      (is (vector? (:documents result))))))

;; ── Derived UI state ───────────────────────────────────────────────────────

(deftest badges-derive-from-desired-state
  (let [decoded (pw/decode-cms-document-wire (get-in list-response [:documents 0]))]
    (testing "selected/published gardens come from :desired, not a stored list"
      (is (= [:knoxx.docs/promethean] (pw/published-garden-ids decoded))))
    (testing "a withheld publication contributes no published garden"
      (is (not (contains? (set (pw/published-garden-ids decoded))
                          :knoxx.docs/legacy))))))

(deftest drift-is-desired-versus-observed
  (testing "published with nothing observed is drift"
    (is (true? (pw/drifted? {:desired :published :observed nil}))))
  (testing "withheld with something still observed is drift"
    (is (true? (pw/drifted? {:desired :withheld :observed "abc123"}))))
  (testing "agreement is not drift"
    (is (false? (pw/drifted? {:desired :published :observed "abc123"})))
    (is (false? (pw/drifted? {:desired :withheld :observed nil})))))
