(ns knoxx.frontend.lib.edn-test
  "Written FIRST (TDD) — defines the parse/serialize contract for the CLJS
  replacement of src/lib/edn.ts (hand-rolled TS parser, no test baseline).
  Contract shape mirrors the real cms/drafts/*/view-contract.edn files,
  including namespaced keywords (:view/id) which the TS parser mangles."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.frontend.lib.edn :as edn]))

(def sample-contract
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

(deftest parses-scalars
  (is (= "hi" (edn/parse-edn "\"hi\"")))
  (is (= 42 (edn/parse-edn "42")))
  (is (= 1.5 (edn/parse-edn "1.5")))
  (is (true? (edn/parse-edn "true")))
  (is (false? (edn/parse-edn "false")))
  (is (nil? (edn/parse-edn "nil")))
  (is (= :draft (edn/parse-edn ":draft")))
  (is (= :view/id (edn/parse-edn ":view/id")) "namespaced keywords survive"))

(deftest parses-collections-commas-and-comments
  (is (= {:a 1 :b [1 2 3]}
         (edn/parse-edn "{:a 1, :b [1, 2, 3]}"))
      "commas are whitespace")
  (is (= {:a 1}
         (edn/parse-edn ";; leading comment\n{:a 1} ;; trailing"))
      "comments ignored"))

(deftest parses-real-view-contract-shape
  (let [contract (edn/parse-edn sample-contract)]
    (is (= "error-coded-radio" (:view/id contract)))
    (is (= :playlist-page (:view/kind contract)))
    (is (= 1 (:view/schema-version contract)))
    (is (= :markdown-import (get-in contract [:source :kind])))
    (is (= [:hero :rich-text] (get-in contract [:layout :zones 0 :accepts])))
    (is (= 2 (count (:blocks contract))))
    (is (= 180 (get-in contract [:blocks 1 :props :tracks 0 :duration])))
    (is (true? (get-in contract [:blocks 1 :props :show_labels])))
    (is (nil? (get-in contract [:publishing :last-published-at])))))

(deftest serializes-scalars
  (is (= "nil" (edn/serialize-edn nil)))
  (is (= "42" (edn/serialize-edn 42)))
  (is (= "true" (edn/serialize-edn true)))
  (is (= "\"hi\"" (edn/serialize-edn "hi")))
  (is (= ":view/id" (edn/serialize-edn :view/id))))

(deftest serializes-strings-with-escapes-readably
  (let [s "say \"hi\"\nthen newline"]
    (is (= s (edn/parse-edn (edn/serialize-edn s)))
        "quotes and newlines survive a round-trip")))

(deftest round-trips-the-real-contract
  (let [contract (edn/parse-edn sample-contract)]
    (is (= contract (edn/parse-edn (edn/serialize-edn contract)))
        "serialize → parse is identity on a real contract")))
