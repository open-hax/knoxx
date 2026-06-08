(ns knoxx.frontend.lib.document-links-test
  "cljs.test parity for the ported document-links logic. Mirrors the cases in
   src/lib/document-links.test.ts so the CLJS impl is the verified canonical one
   before the TypeScript copy is retired."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.frontend.lib.document-links :as sut]))

(deftest detects-external-hrefs
  (is (true? (sut/external-href? "https://example.com")))
  (is (true? (sut/external-href? "mailto:test@example.com")))
  (is (true? (sut/external-href? "tel:+15551234")))
  (is (true? (sut/external-href? "//cdn.example.com/x")))
  (is (false? (sut/external-href? "docs/readme.md")))
  (is (false? (sut/external-href? "#anchor"))))

(deftest normalizes-relative-document-paths
  (is (= "guides/intro.md" (sut/normalize-relative-doc-path "./docs/../guides/intro.md")))
  (is (= "docs/guides/intro.md" (sut/normalize-relative-doc-path "/docs//guides/./intro.md")))
  (testing "leading .. cannot escape above root"
    (is (= "a" (sut/normalize-relative-doc-path "../../a")))))

(deftest resolves-relative-and-absolute-document-hrefs
  (is (= "docs/api/reference.md"
         (sut/resolve-document-href "docs/guides/intro.md" "../api/reference.md")))
  (is (= "docs/index.md"
         (sut/resolve-document-href "docs/guides/intro.md" "/docs/index.md")))
  (is (nil? (sut/resolve-document-href "docs/guides/intro.md" "#overview")))
  (is (nil? (sut/resolve-document-href "docs/guides/intro.md" "https://example.com")))
  (testing "hash and query fragments are stripped before resolving"
    (is (= "docs/api/reference.md"
           (sut/resolve-document-href "docs/guides/intro.md" "../api/reference.md?v=1#top"))))
  (testing "blank and whitespace-only hrefs resolve to nil"
    (is (nil? (sut/resolve-document-href "docs/x.md" "")))
    (is (nil? (sut/resolve-document-href "docs/x.md" "   ")))))
