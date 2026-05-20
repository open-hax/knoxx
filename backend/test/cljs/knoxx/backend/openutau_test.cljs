(ns knoxx.backend.openutau-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.openutau.openutau :as openutau]))

(deftest normalize-notes-assigns-sequential-positions-and-respects-explicit-gaps
  (testing "notes without explicit positions are sequenced after the previous note end"
    (is (= [{:position 0 :duration 480 :tone 60 :lyric "la"}
            {:position 480 :duration 240 :tone 62 :lyric "[l aa]"}
            {:position 1440 :duration 480 :tone 64 :lyric "mi [m iy]"}]
           (openutau/normalize-notes [{:lyric "la" :tone 60 :duration 480}
                                      {:phonetic_hint "l aa" :tone 62 :duration 240}
                                      {:lyric "mi" :phonetic_hint "m iy" :tone 64 :duration 480 :position 1440}])))))

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
      (is (str/includes? yaml "singer: 'ExampleSinger'"))
      (is (str/includes? yaml "phonemizer: 'OpenUtau.Plugin.Builtin.DefaultPhonemizer'"))
      (is (str/includes? yaml "voice_parts:"))
      (is (str/includes? yaml "lyric: '+'")))))
