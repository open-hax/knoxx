(ns knoxx.backend.tools.music-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.tools.music :as music]))

(deftest ensure-json-string-accepts-native-json-object
  (testing "AI tool callers may pass spec_json as an object even when the schema says string"
    (let [json-text (#'music/ensure-json-string!
                     "spec_json"
                     #js {:bpm 110
                          :tracks #js [#js {:instrument "bass"
                                             :patterns #js [#js {:notes #js [#js {:time "0:0:0"
                                                                                   :note "C2"
                                                                                   :duration "4n"}]}]}]})
          parsed (.parse js/JSON json-text)]
      (is (= 110 (aget parsed "bpm")))
      (is (= "bass" (aget (aget (aget parsed "tracks") 0) "instrument")))
      (is (str/includes? json-text "0:0:0")))))

(deftest ensure-json-string-validates-json-strings
  (testing "already-string JSON is preserved after validation"
    (let [json-text "{\"bpm\":120,\"tracks\":[]}"
          result (#'music/ensure-json-string! "spec_json" json-text)]
      (is (= json-text result))))
  (testing "the previous [object Object] coercion path fails fast with a useful message"
    (try
      (#'music/ensure-json-string! "spec_json" "[object Object]")
      (is false "expected invalid JSON to throw")
      (catch :default err
        (is (str/includes? (.-message err) "Invalid spec_json"))))))
