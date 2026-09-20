(ns knoxx.frontend.pages.source-doc.forum-thread-test
  "cljs.test coverage for the ported forum-thread logic. Real (not shim) coverage
   of the meatiest pure logic in the SourceDocPage subtree, ahead of the view
   migration that will consume it."
  (:require [cljs.test :as t]
            [knoxx.frontend.pages.source-doc.forum-thread :as sut]))

(t/deftest parse-forum-thread-guards-path-and-shape
  (t/testing "only .json paths with a posts array parse"
    (t/is (= {:posts [{:postId "p1"}]}
           (sut/parse-forum-thread "thread.json" "{\"posts\":[{\"postId\":\"p1\"}]}")))
    (t/is (nil? (sut/parse-forum-thread "notes.md" "{\"posts\":[]}"))
        "non-json path is rejected even with valid content")
    (t/is (nil? (sut/parse-forum-thread "thread.json" "{\"posts\":\"nope\"}"))
        "posts must be an array")
    (t/is (nil? (sut/parse-forum-thread "thread.json" "not json"))
        "invalid JSON returns nil, not a throw"))
  (t/testing "json extension match is case-insensitive"
    (t/is (some? (sut/parse-forum-thread "THREAD.JSON" "{\"posts\":[]}")))))

(t/deftest extract-inline-image-urls-covers-all-forms
  (t/is (= [] (sut/extract-inline-image-urls "")))
  (t/is (= [] (sut/extract-inline-image-urls nil)))
  (t/testing "bbcode, markdown, and bare image links are all extracted"
    (t/is (= ["https://x.test/a.png"]
           (sut/extract-inline-image-urls "[img]https://x.test/a.png[/img]")))
    (t/is (= ["https://x.test/b.jpg"]
           (sut/extract-inline-image-urls "![alt](https://x.test/b.jpg)")))
    (t/is (= ["https://x.test/c.webp"]
           (sut/extract-inline-image-urls "see https://x.test/c.webp here"))))
  (t/testing "results are de-duplicated, order-preserving"
    (t/is (= ["https://x.test/a.png" "https://x.test/b.gif"]
           (sut/extract-inline-image-urls
            "[img]https://x.test/a.png[/img] https://x.test/b.gif [img]https://x.test/a.png[/img]")))))

(t/deftest format-post-date-prefers-raw-then-guards
  (t/is (= "yesterday" (sut/format-post-date {:rawDate "yesterday" :date "2020-01-01"})))
  (t/is (= "Unknown date" (sut/format-post-date {})))
  (t/is (= "Unknown date" (sut/format-post-date {:date "not-a-date"})))
  (t/is (string? (sut/format-post-date {:date "2020-01-01T00:00:00Z"})))
  (t/is (not= "Unknown date" (sut/format-post-date {:date "2020-01-01T00:00:00Z"}))))

(t/deftest build-prepared-posts-derives-render-model
  (let [prepared (sut/build-prepared-posts
                  {:posts [{:postId "p1" :contentFull "hello [img]https://x.test/a.png[/img]"
                            :images ["https://x.test/declared.jpg"]}
                           {:content "second"}]})]
    (t/testing "labels and keys fall back to index when postId is absent"
      (t/is (= ["p1" "#2"] (mapv :post-label prepared)))
      (t/is (= ["p1" "1"] (mapv :post-key prepared))))
    (t/testing "body prefers contentFull, then content"
      (t/is (= "hello [img]https://x.test/a.png[/img]" (:body (first prepared))))
      (t/is (= "second" (:body (second prepared)))))
    (t/testing "declared + inline images merge, de-duplicated"
      (t/is (= ["https://x.test/declared.jpg" "https://x.test/a.png"]
             (:image-urls (first prepared))))
      (t/is (= [] (:image-urls (second prepared)))))))
