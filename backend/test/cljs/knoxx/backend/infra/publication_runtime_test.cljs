(ns knoxx.backend.infra.publication-runtime-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.translation-evidence :as evidence]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.publication-runtime :as runtime]
            [knoxx.backend.infra.translation-agent-content :as agent-content]
            [knoxx.backend.infra.translation-content-integrity :as integrity]))

(def ^:private translated "Primer bloque.\n\nSegundo bloque.")

(def ^:private receipt
  {:receipt/type :translation/completed
   :translation/document :knoxx.docs/probe
   :translation/garden :knoxx.gardens/promethean
   :translation/source-locale :en
   :translation/locale :es
   :translation/source-revision "sha256-source"
   :translation/revision "candidate-1"
   :translation/content-digest (integrity/content-digest translated)
   :translation/dispatch-key "candidate-1"
   :translation/org-id "org-1"
   :translation/project "review-stage"
   :translation/at "2026-08-30T10:00:00.000Z"})

(def ^:private document
  {:document/id :knoxx.docs/probe
   :document/source-locale :en
   :document/translations {:es {:path "probe.es.md"}}})

(def ^:private intent
  {:publication/document :knoxx.docs/probe
   :publication/garden :knoxx.gardens/promethean
   :publication/locale :es})

(defn- ^:async translated-blocks
  [agent-value authored-value legacy-calls
   & [candidate-receipt legacy-segments]]
  (let [candidate-receipt (or candidate-receipt receipt)
        legacy-segments (or legacy-segments [{:translated_text translated}])]
    (with-redefs [agent-content/content-for-receipt!
                  (fn [_ _] (js/Promise.resolve agent-value))
                  contract-content/localized-content!
                  (fn [_ _ _] (js/Promise.resolve authored-value))
                  openplanner-client/translation-document!
                  (fn [& _]
                    (swap! legacy-calls inc)
                    (js/Promise.resolve {:segments legacy-segments}))]
      (await (#'runtime/translated-blocks!
              ::client
              {:org-id "org-1" :project "review-stage"}
              {:knoxx.docs/probe "/contracts"}
              document intent candidate-receipt "/published")))))

(deftest ^:async publication-renders-only-receipt-bound-target-bytes
  (testing "exact agent bytes are admitted without a weaker fallback"
    (let [legacy-calls (atom 0)]
      (is (= ["Primer bloque." "Segundo bloque."]
             (await (translated-blocks translated "Authored" legacy-calls))))
      (is (zero? @legacy-calls))))

  (testing "tampered agent bytes stop instead of falling through"
    (let [legacy-calls (atom 0)]
      (is (nil? (await (translated-blocks "Changed" translated legacy-calls))))
      (is (zero? @legacy-calls))))

  (testing "an edited authored file cannot reuse its old receipt and approval"
    (let [legacy-calls (atom 0)]
      (is (nil? (await (translated-blocks nil "Changed" legacy-calls))))
      (is (zero? @legacy-calls))))

  (testing "the exact authored bytes remain a lawful fallback"
    (let [legacy-calls (atom 0)]
      (is (= ["Primer bloque." "Segundo bloque."]
             (await (translated-blocks nil translated legacy-calls))))
      (is (zero? @legacy-calls)))))

(deftest ^:async legacy-openplanner-segments-have-authenticated-boundaries
  (let [segments [{:translated_text "Primer segmento."}
                  {:translated_text "Segundo segmento."}]
        joined "Primer segmento.\n\nSegundo segmento."
        joined-receipt (assoc receipt
                              :translation/content-digest
                              (integrity/content-digest joined))
        legacy-calls (atom 0)]
    (is (= ["Primer segmento." "Segundo segmento."]
           (await (translated-blocks nil nil legacy-calls
                                     joined-receipt segments))))
    (is (= 1 @legacy-calls))
    (testing "the former undelimited bytes cannot reuse the joined receipt"
      (let [undelimited-receipt (assoc receipt
                                       :translation/content-digest
                                       (integrity/content-digest
                                        "Primer segmento.Segundo segmento."))]
        (is (nil? (await (translated-blocks nil nil legacy-calls
                                            undelimited-receipt segments))))
        (is (= 2 @legacy-calls))))))

(deftest ^:async rendered-translation-retains-separate-source-and-output-revisions
  (with-redefs [agent-content/content-for-receipt! (fn [_ _] (js/Promise.resolve translated))]
    (let [render! (runtime/artifact-source ::client {:org-id "org-1" :project "review-stage"}
                                           {:documents {:knoxx.docs/probe document}}
                                           {:knoxx.docs/probe "/contracts"}
                                           (evidence/evidence {:receipts [receipt]}) "/published")
          artifact (await (render! intent "sha256-source"))]
      (is (= "sha256-source" (:artifact/revision artifact)))
      (is (= "candidate-1" (:artifact/content-revision artifact)))
      (is (= "<article class=\"published-document\"><p>Primer bloque.</p><p>Segundo bloque.</p></article>"
             (:artifact/content artifact))))))

(deftest approved-document-media-references-render-without-remote-player-loading
  (let [image "![Sutured Signal](/graphics/Sutured_Signal.svg)"
        song "[We Are The Place](https://suno.com/song/a7d0fb41-785e-4919-84d8-0c076814192b)"
        fragment (runtime/render-fragment ["Selected art and music." image song])]
    (is (str/includes? fragment "<img src=\"/graphics/Sutured_Signal.svg\" alt=\"Sutured Signal\" loading=\"lazy\">"))
    (is (str/includes? fragment "<a href=\"https://suno.com/song/a7d0fb41-785e-4919-84d8-0c076814192b\" rel=\"noopener noreferrer\">We Are The Place</a>"))
    (is (not (str/includes? fragment "<iframe")))
    (is (not (str/includes? fragment "<script")))
    (is (not (str/includes? fragment "autoplay")))))

(deftest native-audio-refers-only-to-staged-same-origin-tracks
  (let [fragment (runtime/render-fragment ["[A local track](/music/album/track.wav)"])]
    (is (str/includes? fragment "<audio controls preload=\"none\" src=\"/music/album/track.wav\"></audio>"))
    (is (str/includes? fragment "<a href=\"/music/album/track.wav\">A local track</a>"))
    (is (not (str/includes? fragment "autoplay")))))

(deftest media-at-document-end-retains-normal-markdown-line-endings
  (doseq [newline ["\n" "\r\n"]]
    (is (str/includes? (runtime/render-fragment [(str "![Art](/graphics/Sutured_Signal.svg)" newline)])
                       "<img src=\"/graphics/Sutured_Signal.svg\""))
    (is (str/includes? (runtime/render-fragment [(str "[Music](https://suno.com/song/a7d0fb41-785e-4919-84d8-0c076814192b)" newline)])
                       "<a href=\"https://suno.com/song/"))
    (is (str/includes? (runtime/render-fragment [(str "[Local](/music/track.wav)" newline)])
                       "<audio controls preload=\"none\""))))

(deftest publication-media-fails-closed-on-nonpublic-or-active-references
  (doseq [block ["![Private](/published/.history/source.svg)"
                 "![Private](/api/workspace-media/raw?path=private.svg)"
                 "![Outside](https://example.com/track.svg)"
                 "![Scheme relative](//example.com/track.svg)"
                 "![Traversal](/graphics/../private.svg)"
                 "![Encoded traversal](/graphics/%2e%2e/private.svg)"
                 "![Encoded slash](/graphics/a%2fb.svg)"
                 "![Control](/graphics/a\u0000.svg)"
                 "![Query](/graphics/a.svg?token=private)"
                 "![Fragment](/graphics/a.svg#active)"
                 "![Backslash](/graphics/a\\b.svg)"
                 "![Unsupported](/graphics/a.html)"
                 "[Wrong host](https://suno.com.evil/song/a7d0fb41-785e-4919-84d8-0c076814192b)"
                 "[Userinfo](https://suno.com@evil/song/a7d0fb41-785e-4919-84d8-0c076814192b)"
                 "[Extra query](https://suno.com/song/a7d0fb41-785e-4919-84d8-0c076814192b?token=private)"
                 "[Embed](https://suno.com/embed/a7d0fb41-785e-4919-84d8-0c076814192b)"
                 "[Script](javascript:alert(1))"
                 "[External audio](https://example.com/private.wav)"
                 "[Private audio](/published/private.wav)"
                 "<img src=x onerror=alert(1)>"
                 "<script>alert(1)</script>"
                 "> ![Quoted](/graphics/Sutured_Signal.svg)"
                 "    ![Indented code](/graphics/Sutured_Signal.svg)"]]
    (let [fragment (runtime/render-fragment [block])]
      (is (not (re-find #"<(?:img|audio|a|iframe|script)(?: |>)" fragment)) block))))

(deftest prose-labels-and-code-fences-retain-their-literal-meaning
  (is (= "<article class=\"published-document\"><p>Ordinary &amp; &lt;unsafe&gt;<br>second line.</p></article>"
         (runtime/render-fragment ["Ordinary & <unsafe>\nsecond line."])))
  (let [image "![<art & \"quoted\">](/graphics/Sutured_Signal.svg)"
        song "[<music & \"quoted\">](https://suno.com/song/a7d0fb41-785e-4919-84d8-0c076814192b)"
        fragment (runtime/render-fragment [image song])]
    (is (str/includes? fragment "alt=\"&lt;art &amp; &quot;quoted&quot;&gt;\""))
    (is (str/includes? fragment "&lt;music &amp; &quot;quoted&quot;&gt;</a>")))
  (doseq [marker ["```" "~~~~"]]
    (let [fragment (runtime/render-fragment [(str marker "markdown")
                                             "![A code example](/graphics/Sutured_Signal.svg)"
                                             marker
                                             "![An actual image](/graphics/Sutured_Signal.svg)"])]
      (is (= 1 (count (re-seq #"<img " fragment))))
      (is (str/includes? fragment "<p>![A code example](/graphics/Sutured_Signal.svg)</p>")))))

(deftest ^:async reviewed-media-comes-from-exact-authenticated-target-text
  (let [target "Obras seleccionadas.\n\n![Sutured Signal](/graphics/Sutured_Signal.svg)\n\n[We Are The Place](https://suno.com/song/a7d0fb41-785e-4919-84d8-0c076814192b)"
        exact-receipt (assoc receipt :translation/content-digest (integrity/content-digest target))
        render! (runtime/artifact-source ::client {:org-id "org-1" :project "review-stage"}
                                         {:documents {:knoxx.docs/probe (assoc document :metadata {:blocks [{:type "image" :src "/graphics/unapproved.svg"}]})}}
                                         {:knoxx.docs/probe "/contracts"}
                                         (evidence/evidence {:receipts [exact-receipt]}) "/published")]
    (with-redefs [agent-content/content-for-receipt! (fn [_ _] (js/Promise.resolve target))]
      (let [artifact (await (render! intent "sha256-source"))]
        (is (str/includes? (:artifact/content artifact) "<img src=\"/graphics/Sutured_Signal.svg\""))
        (is (str/includes? (:artifact/content artifact) "https://suno.com/song/a7d0fb41-785e-4919-84d8-0c076814192b"))
        (is (not (str/includes? (:artifact/content artifact) "unapproved.svg")))
        (is (= "sha256-source" (:artifact/revision artifact)))
        (is (= "candidate-1" (:artifact/content-revision artifact)))))
    (with-redefs [agent-content/content-for-receipt! (fn [_ _] (js/Promise.resolve (str/replace target "Sutured_Signal.svg" "unapproved.svg")))]
      (is (nil? (await (render! intent "sha256-source")))))))
