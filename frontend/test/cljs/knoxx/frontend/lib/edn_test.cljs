(ns knoxx.frontend.lib.edn-test
  "Written FIRST (TDD) — defines the parse/serialize contract for the CLJS
  replacement of src/lib/edn.ts (hand-rolled TS parser, no test baseline).
  Contract shape mirrors the real cms/drafts/*/view-contract.edn files,
  including namespaced keywords (:view/id) which the TS parser mangles."
  (:require [cljs.test :as t]
            [knoxx.frontend.lib.edn :as edn]))

(def ^:private sample-contract
  "{:view/id \"error-coded-radio\"
    :view/title \"Error Coded Radio\"
    :view/kind :playlist-page
    :view/schema-version 1
    :view/status :draft

    ;; comments are ignored
    :source {:kind :markdown-import
             :path \"docs/notes/error-coded-radio.md\"}

    :layout {:template :studio-playlist-page
             :zones [{:id :hero, :label \"Hero\", :accepts [:hero :rich-text]}
                     {:id :main, :label \"Main content\", :accepts [:playlist]}]}

    :blocks [{:id \"hero\"
              :type :hero
              :zone :hero
              :props {:title \"Error Coded Radio\"
                      :subtitle \"A playlist from Broadcast Studio\"}}
             {:id \"playlist\"
              :type :playlist
              :zone :main
              :props {:tracks [{:path \"audio/a.mp3\" :duration 180}
                               {:path \"audio/b.mp3\" :duration 240}]
                      :show_labels true}}]

    :publishing {:last-published-at nil
                 :defer-index true}}")

(t/deftest parses-scalars
  (t/is (= "hi" (edn/parse-edn "\"hi\"")))
  (t/is (= 42 (edn/parse-edn "42")))
  (t/is (= 1.5 (edn/parse-edn "1.5")))
  (t/is (true? (edn/parse-edn "true")))
  (t/is (false? (edn/parse-edn "false")))
  (t/is (nil? (edn/parse-edn "nil")))
  (t/is (= :draft (edn/parse-edn ":draft")))
  (t/is (= :view/id (edn/parse-edn ":view/id")) "namespaced keywords survive"))

(t/deftest parses-collections-commas-and-comments
  (t/is (= {:a 1 :b [1 2 3]}
         (edn/parse-edn "{:a 1, :b [1, 2, 3]}"))
      "commas are whitespace")
  (t/is (= {:a 1}
         (edn/parse-edn ";; leading comment\n{:a 1} ;; trailing"))
      "comments ignored"))

(t/deftest parses-real-view-contract-shape
  (let [contract (edn/parse-edn sample-contract)]
    (t/is (= "error-coded-radio" (:view/id contract)))
    (t/is (= :playlist-page (:view/kind contract)))
    (t/is (= 1 (:view/schema-version contract)))
    (t/is (= :markdown-import (get-in contract [:source :kind])))
    (t/is (= [:hero :rich-text] (get-in contract [:layout :zones 0 :accepts])))
    (t/is (= 2 (count (:blocks contract))))
    (t/is (= 180 (get-in contract [:blocks 1 :props :tracks 0 :duration])))
    (t/is (true? (get-in contract [:blocks 1 :props :show_labels])))
    (t/is (nil? (get-in contract [:publishing :last-published-at])))))

(t/deftest serializes-scalars
  (t/is (= "nil" (edn/serialize-edn nil)))
  (t/is (= "42" (edn/serialize-edn 42)))
  (t/is (= "true" (edn/serialize-edn true)))
  (t/is (= "\"hi\"" (edn/serialize-edn "hi")))
  (t/is (= ":view/id" (edn/serialize-edn :view/id))))

(t/deftest serializes-strings-with-escapes-readably
  (let [s "say \"hi\"\nthen newline"]
    (t/is (= s (edn/parse-edn (edn/serialize-edn s)))
        "quotes and newlines survive a round-trip")))

(t/deftest round-trips-the-real-contract
  (let [contract (edn/parse-edn sample-contract)]
    (t/is (= contract (edn/parse-edn (edn/serialize-edn contract)))
        "serialize → parse is identity on a real contract")))
