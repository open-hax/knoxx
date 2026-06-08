(ns knoxx.frontend.pages.source-doc.forum-thread-test
  "cljs.test coverage for the ported forum-thread logic. Real (not shim) coverage
   of the meatiest pure logic in the SourceDocPage subtree, ahead of the view
   migration that will consume it."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.frontend.pages.source-doc.forum-thread :as sut]))

(deftest parse-forum-thread-guards-path-and-shape
  (testing "only .json paths with a posts array parse"
    (is (= {:posts [{:postId "p1"}]}
           (sut/parse-forum-thread "thread.json" "{\"posts\":[{\"postId\":\"p1\"}]}")))
    (is (nil? (sut/parse-forum-thread "notes.md" "{\"posts\":[]}"))
        "non-json path is rejected even with valid content")
    (is (nil? (sut/parse-forum-thread "thread.json" "{\"posts\":\"nope\"}"))
        "posts must be an array")
    (is (nil? (sut/parse-forum-thread "thread.json" "not json"))
        "invalid JSON returns nil, not a throw"))
  (testing "json extension match is case-insensitive"
    (is (some? (sut/parse-forum-thread "THREAD.JSON" "{\"posts\":[]}")))))

(deftest extract-inline-image-urls-covers-all-forms
  (is (= [] (sut/extract-inline-image-urls "")))
  (is (= [] (sut/extract-inline-image-urls nil)))
  (testing "bbcode, markdown, and bare image links are all extracted"
    (is (= ["https://x.test/a.png"]
           (sut/extract-inline-image-urls "[img]https://x.test/a.png[/img]")))
    (is (= ["https://x.test/b.jpg"]
           (sut/extract-inline-image-urls "![alt](https://x.test/b.jpg)")))
    (is (= ["https://x.test/c.webp"]
           (sut/extract-inline-image-urls "see https://x.test/c.webp here"))))
  (testing "results are de-duplicated, order-preserving"
    (is (= ["https://x.test/a.png" "https://x.test/b.gif"]
           (sut/extract-inline-image-urls
            "[img]https://x.test/a.png[/img] https://x.test/b.gif [img]https://x.test/a.png[/img]")))))

(deftest format-post-date-prefers-raw-then-guards
  (is (= "yesterday" (sut/format-post-date {:rawDate "yesterday" :date "2020-01-01"})))
  (is (= "Unknown date" (sut/format-post-date {})))
  (is (= "Unknown date" (sut/format-post-date {:date "not-a-date"})))
  (is (string? (sut/format-post-date {:date "2020-01-01T00:00:00Z"})))
  (is (not= "Unknown date" (sut/format-post-date {:date "2020-01-01T00:00:00Z"}))))

(deftest build-prepared-posts-derives-render-model
  (let [prepared (sut/build-prepared-posts
                  {:posts [{:postId "p1" :contentFull "hello [img]https://x.test/a.png[/img]"
                            :images ["https://x.test/declared.jpg"]}
                           {:content "second"}]})]
    (testing "labels and keys fall back to index when postId is absent"
      (is (= ["p1" "#2"] (mapv :post-label prepared)))
      (is (= ["p1" "1"] (mapv :post-key prepared))))
    (testing "body prefers contentFull, then content"
      (is (= "hello [img]https://x.test/a.png[/img]" (:body (first prepared))))
      (is (= "second" (:body (second prepared)))))
    (testing "declared + inline images merge, de-duplicated"
      (is (= ["https://x.test/declared.jpg" "https://x.test/a.png"]
             (:image-urls (first prepared))))
      (is (= [] (:image-urls (second prepared)))))))
