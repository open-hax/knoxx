(ns knoxx.backend.openutau-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.openutau.openutau :as openutau]
            [knoxx.backend.domain.openutau.tools :as tools]))

(def ^:private missing-env-value (js-obj))

(defn- with-env!
  [bindings f]
  (let [env (.-env js/process)
        previous (into {}
                       (map (fn [[k _]]
                              [k (let [value (aget env k)]
                                   (if (some? value) value missing-env-value))]))
                       bindings)]
    (doseq [[k v] bindings]
      (if (nil? v)
        (js-delete env k)
        (aset env k v)))
    (try
      (f)
      (finally
        (doseq [[k old-value] previous]
          (if (identical? old-value missing-env-value)
            (js-delete env k)
            (aset env k old-value)))))))

(deftest render-script-path-is-runtime-configurable
  (with-env! {"KNOXX_OPENUTAU_RENDER_SCRIPT" nil}
    (fn []
      (is (= "render-ustx.sh" (tools/render-script-path)))))
  (with-env! {"KNOXX_OPENUTAU_RENDER_SCRIPT" "/opt/openutau/render-ustx.sh"}
    (fn []
      (is (= "/opt/openutau/render-ustx.sh" (tools/render-script-path))))))

(deftest normalize-notes-assigns-sequential-positions-and-respects-explicit-gaps
  (testing "notes without explicit positions are sequenced after the previous note end"
    (let [notes (openutau/normalize-notes [{:lyric "la" :tone 60 :duration 480}
                                           {:phonetic_hint "l aa" :tone 62 :duration 240}
                                           {:lyric "mi" :phonetic_hint "m iy" :tone 64 :duration 480 :position 1440}])]
      (is (= [{:position 0 :duration 480 :tone 60 :lyric "la"}
              {:position 480 :duration 240 :tone 62 :lyric "[l aa]"}
              {:position 1440 :duration 480 :tone 64 :lyric "mi [m iy]"}]
             (take 3 notes)))
      (is (= 12 (count notes))))))

(deftest project->ustx-yaml-emits-openutau-shaped-fields
  (testing "generated yaml contains the essential OpenUtau project fields"
    (let [notes (openutau/normalize-notes [{:lyric "la" :tone 60 :duration 480}
                                           {:lyric "+" :tone 62 :duration 480}])
          project (openutau/build-project {:project_name "Knoxx Demo"
                                           :tempo 128
                                           :singer_id "ExampleSinger"
                                           :phonemizer "OpenUtau.Plugin.Builtin.DefaultPhonemizer"}
                                          notes)
          yaml (openutau/project->ustx-yaml project)]
      (is (str/includes? yaml "name: 'Knoxx Demo'"))
      (is (str/includes? yaml "ustx_version: '0.6'"))
      (is (str/includes? yaml "renderer: 'WORLDLINE-R'"))
      (is (str/includes? yaml "singer: '重音テト OU用日本語統合ライブラリー'"))
      (is (str/includes? yaml "phonemizer: 'OpenUtau.Plugin.Builtin.DefaultPhonemizer'"))
      (is (str/includes? yaml "voice_parts:"))
      (is (str/includes? yaml "lyric: '+'")))))

(deftest canonical-core-matches-render-tool-normalization
  (testing "OpenUtau core owns render-safe lyric sanitation, note padding, and singer defaults"
    (let [notes (openutau/normalize-notes [{:lyric "kyo ukai" :tone 60 :duration 480}])
          project (openutau/build-project {:project_name "Knoxx Demo"
                                           :singer_id "ritsu"}
                                          notes)
          yaml (openutau/project->ustx-yaml project)]
      (is (= 12 (count notes)))
      (is (= "kaikyou" (:lyric (first notes))))
      (is (= "あ" (:lyric (second notes))))
      (is (str/includes? yaml "singer: '波音リツ連続音Ver1.5.1'"))
      (is (str/includes? yaml "phonemizer: 'OpenUtau.Plugin.Builtin.JapaneseVCVPhonemizer'")))))
