(ns knoxx.backend.law.url-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.law.url :as url]))

(deftest looks-like-url-matches-http-and-https
  (testing "recognizes http:// and https:// prefixes"
    (is (true? (url/looks-like-url? "http://example.com")))
    (is (true? (url/looks-like-url? "https://example.com")))
    (is (true? (url/looks-like-url? "https://example.com/path?query=1")))))

(deftest looks-like-url-rejects-non-urls
  (testing "rejects strings without http/https prefix"
    (is (false? (url/looks-like-url? "example.com")))
    (is (false? (url/looks-like-url? "/local/path")))
    (is (false? (url/looks-like-url? "ftp://example.com")))
    (is (false? (url/looks-like-url? "")))
    (is (false? (url/looks-like-url? nil)))
    (is (false? (url/looks-like-url? 42)))))

(deftest media-url-matches-http-https-and-slash
  (testing "recognizes http, https, and absolute paths"
    (is (true? (url/media-url? "http://example.com")))
    (is (true? (url/media-url? "https://example.com")))
    (is (true? (url/media-url? "/local/path")))
    (is (true? (url/media-url? "/assets/image.png")))))

(deftest media-url-rejects-non-media
  (testing "rejects non-url and non-path strings"
    (is (false? (url/media-url? "example.com")))
    (is (false? (url/media-url? "ftp://example.com")))
    (is (false? (url/media-url? "data:image/png;base64,abc")))
    (is (false? (url/media-url? "")))
    (is (false? (url/media-url? nil)))
    (is (false? (url/media-url? 42)))))

(deftest data-url-matches-data-prefix
  (testing "recognizes data: prefix"
    (is (true? (url/data-url? "data:image/png;base64,abc")))
    (is (true? (url/data-url? "data:text/plain,hello")))))

(deftest data-url-rejects-non-data
  (testing "rejects strings without data: prefix"
    (is (false? (url/data-url? "http://example.com")))
    (is (false? (url/data-url? "/local/path")))
    (is (false? (url/data-url? "")))
    (is (false? (url/data-url? nil)))
    (is (false? (url/data-url? 42)))))
