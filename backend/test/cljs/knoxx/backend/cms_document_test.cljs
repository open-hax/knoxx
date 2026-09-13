(ns knoxx.backend.cms-document-test
  (:require [cljs.reader :as reader]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.cms-document :as cms]
            [knoxx.backend.law.cms-document :as law]))
(deftest paths-and-visibility-are-not-client-authority
  (doseq [id ["../other" "/root" "org/file" "" nil]]
    (is (thrown? cljs.core/ExceptionInfo (law/require-id! id))))
  (doseq [body [{:title "" :content "text"} {:title "Title" :content nil}
                {:title "Title" :content "text" :visibility "public"}]]
    (is (thrown? cljs.core/ExceptionInfo (law/require-body! body))))
  (let [record (cms/record "org-one" "doc-one" "/trusted/doc-one.md"
                  {:title "Title" :content "Text" :visibility "review" :source_path "/etc/passwd" :metadata {:admin true}} nil)]
    (is (= "/trusted/doc-one.md" (:source_path record)))
    (is (= {} (:metadata record)))
    (is (= "cms.org-one/workspace" (:garden_id record)))))
(deftest new-publications-are-private-and-withheld
  (let [resources (:resources (cms/manifest "org-one" "doc-one" "/trusted/doc.md" "Title"))]
    (is (= "org-one" (:document/org-id (first resources))))
    (is (= :private (:document/visibility (first resources))))
    (is (= #{:withheld} (set (map :publication/state (rest resources)))))
    (is (= #{:en :es} (set (map :publication/locale (rest resources)))))))

(deftest generated-resources-survive-edn-round-trip
  (let [manifest (cms/manifest "ad0ce178-d0af-4b45-849e-44b2fb0a180b" "0caf5da5-1629-42f6-970a-423f6fdf5cc5" "/trusted/doc.md" "Title")]
    (is (= manifest (reader/read-string (pr-str manifest))))))
