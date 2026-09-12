(ns knoxx.frontend.lib.publication-wire-test
  (:require [cljs.test :as t]
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

(def ^:private list-response
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

(t/deftest ^:async publish-request-body-matches-the-shared-contract
  (t/testing "the body is built from the same vocabulary the backend contract is
            built from, and it survives api/request's own serialization"
    (let [{:keys [calls]} (await (with-fetch-stub
                                   {}
                                   #(pw/publish! :knoxx.docs/probe-es)))
          body (sent-body calls)]
      (t/is (= 1 (count calls)))
      (t/is (= "PATCH" (:method (first calls))))
      (t/testing "the wire key is unqualified — a namespace would not survive clj->js"
        (t/is (= [wire/state-patch-key] (keys body)))
        (t/is (= :state wire/state-patch-key))
        (t/is (not (contains? body :publication/state))))
      (t/testing "and the value is the shared enum string"
        (t/is (= (wire/encode-state :published) (get body wire/state-patch-key)))
        (t/is (= "published" (get body wire/state-patch-key))))
      (t/testing "which decodes back to the canonical domain state"
        (t/is (= :published (wire/decode-state (get body wire/state-patch-key))))))))

(t/deftest ^:async every-state-edit-sends-a-contract-valid-body
  (doseq [[helper expected] [[pw/publish! :published]
                             [pw/unpublish! :withheld]
                             [pw/archive! :archived]]]
    (t/testing (str expected)
      (let [{:keys [calls]} (await (with-fetch-stub {} #(helper :knoxx.docs/probe-es)))
            body (sent-body calls)]
        (t/is (= (wire/encode-state expected) (get body wire/state-patch-key)))
        (t/is (contains? (set wire/state-wire-values)
                       (get body wire/state-patch-key)))))))

(t/deftest ^:async draft-round-trips-through-cms-get-and-state-patch
  (let [draft-response (assoc-in list-response
                                 [:documents 0 :publications 0 :desired]
                                 "draft")
        {:keys [result]} (await (with-fetch-stub draft-response #(pw/load-cms!)))]
    (t/testing "a draft row returned by CMS GET is decoded as desired state"
      (t/is (= :draft (get-in result [:documents 0 :publications 0 :desired])))))
  (let [{:keys [calls]} (await (with-fetch-stub
                                {}
                                #(pw/set-publication-state!
                                  :knoxx.docs/probe-es
                                  :draft)))
        body (sent-body calls)]
    (t/testing "the generic state PATCH sends the shared draft spelling"
      (t/is (= {:state "draft"} body))
      (t/is (= :draft (wire/decode-state (:state body)))))))

(t/deftest ^:async patch-url-carries-no-encoded-colon
  (let [{:keys [calls]} (await (with-fetch-stub {} #(pw/publish! :knoxx.docs/probe-es)))
        path (:path (first calls))]
    (t/is (str/includes? path "knoxx.docs"))
    (t/testing "a qualified id encodes without an EDN leading colon, so no %3A"
      (t/is (not (str/includes? path "%3A")))
      (t/is (not (str/includes? path ":"))))))

;; ── Identity round trip ────────────────────────────────────────────────────

(t/deftest resource-id-round-trip
  (doseq [id [:docs/probe :knoxx.docs/translation-pipeline :bare]]
    (t/testing (str id)
      (t/is (= id (pw/decode-id (pw/encode-id id))))
      (t/is (not (str/starts-with? (pw/encode-id id) ":")))))
  (t/testing "docs/probe encodes exactly, with no colon"
    (t/is (= "docs/probe" (pw/encode-id :docs/probe)))
    (t/is (= :docs/probe (pw/decode-id "docs/probe"))))
  (t/testing "namespaces do not collapse"
    (t/is (not= (pw/encode-id :tenant-a/foo) (pw/encode-id :tenant-b/foo)))))

;; ── Row decoding ───────────────────────────────────────────────────────────

(t/deftest ^:async load-cms!-populates-from-a-normalized-response
  (let [{:keys [result]} (await (with-fetch-stub list-response #(pw/load-cms!)))]
    (t/testing "the response is not double-wrapped"
      (t/is (= #{:documents :gardens} (set (keys result))))
      (t/is (= #{:document :publications} (set (keys (first (:documents result)))))))
    (let [document (get-in result [:documents 0 :document])
          [spanish french] (get-in result [:documents 0 :publications])
          [active archived] (:gardens result)]
      (t/testing "document row"
        (t/is (= :knoxx.docs/probe (:id document)))
        (t/is (= :en (:source-locale document)))
        (t/is (= "docs/probe.md" (get-in document [:source :path]))))
      (t/testing "garden rows decode status to keywords"
        (t/is (= :knoxx.docs/promethean (:id active)))
        (t/is (= :active (:status active)))
        (t/is (= :archived (:status archived))))
      (t/testing "publication rows decode every keyword-valued field"
        (t/is (= :knoxx.docs/probe-es (:id spanish)))
        (t/is (= :knoxx.docs/probe (:document spanish)))
        (t/is (= :knoxx.docs/promethean (:garden spanish)))
        (t/is (= :es (:locale spanish)))
        (t/is (= :published (:desired spanish)))
        (t/is (= [:translation-missing] (:blockers spanish))))
      (t/testing "a selector revision decodes to a keyword, a concrete one stays a string"
        (t/is (= :source/current (:revision spanish)))
        (t/is (= "abc123" (:revision french)))
        (t/is (string? (:revision french))))
      (t/testing "observed is runtime evidence and stays as-is"
        (t/is (nil? (:observed spanish)))
        (t/is (= "abc123" (:observed french)))))))

(t/deftest ^:async loader-uses-native-async-not-promise-chains
  (t/testing "load-cms! awaits rather than chaining"
    (let [{:keys [result]} (await (with-fetch-stub list-response #(pw/load-cms!)))]
      (t/is (map? result) "a .then chain would have resolved to a promise here")
      (t/is (vector? (:documents result))))))

;; ── Derived UI state ───────────────────────────────────────────────────────

(t/deftest badges-derive-from-desired-state
  (let [decoded (pw/decode-cms-document-wire (get-in list-response [:documents 0]))]
    (t/testing "selected/published gardens come from :desired, not a stored list"
      (t/is (= [:knoxx.docs/promethean] (pw/published-garden-ids decoded))))
    (t/testing "a withheld publication contributes no published garden"
      (t/is (not (contains? (set (pw/published-garden-ids decoded))
                          :knoxx.docs/legacy))))))

(t/deftest drift-is-desired-versus-observed
  (t/testing "published with nothing observed is drift"
    (t/is (true? (pw/drifted? {:desired :published :observed nil}))))
  (t/testing "withheld with something still observed is drift"
    (t/is (true? (pw/drifted? {:desired :withheld :observed "abc123"}))))
  (t/testing "agreement is not drift"
    (t/is (false? (pw/drifted? {:desired :published :observed "abc123"})))
    (t/is (false? (pw/drifted? {:desired :withheld :observed nil})))))
