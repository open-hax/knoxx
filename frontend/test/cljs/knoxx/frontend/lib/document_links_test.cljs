(ns knoxx.frontend.lib.document-links-test
  "cljs.test parity for the ported document-links logic. Mirrors the cases in
   src/lib/document-links.test.ts so the CLJS impl is the verified canonical one
   before the TypeScript copy is retired."
  (:require [cljs.test :as t]
            [knoxx.frontend.lib.document-links :as sut]))

(t/deftest detects-external-hrefs
  (t/is (true? (sut/external-href? "https://example.com")))
  (t/is (true? (sut/external-href? "mailto:test@example.com")))
  (t/is (true? (sut/external-href? "tel:+15551234")))
  (t/is (true? (sut/external-href? "//cdn.example.com/x")))
  (t/is (false? (sut/external-href? "docs/readme.md")))
  (t/is (false? (sut/external-href? "#anchor"))))

(t/deftest normalizes-relative-document-paths
  (t/is (= "guides/intro.md" (sut/normalize-relative-doc-path "./docs/../guides/intro.md")))
  (t/is (= "docs/guides/intro.md" (sut/normalize-relative-doc-path "/docs//guides/./intro.md")))
  (t/testing "leading .. cannot escape above root"
    (t/is (= "a" (sut/normalize-relative-doc-path "../../a")))))

(t/deftest resolves-relative-and-absolute-document-hrefs
  (t/is (= "docs/api/reference.md"
         (sut/resolve-document-href "docs/guides/intro.md" "../api/reference.md")))
  (t/is (= "docs/index.md"
         (sut/resolve-document-href "docs/guides/intro.md" "/docs/index.md")))
  (t/is (nil? (sut/resolve-document-href "docs/guides/intro.md" "#overview")))
  (t/is (nil? (sut/resolve-document-href "docs/guides/intro.md" "https://example.com")))
  (t/testing "hash and query fragments are stripped before resolving"
    (t/is (= "docs/api/reference.md"
           (sut/resolve-document-href "docs/guides/intro.md" "../api/reference.md?v=1#top"))))
  (t/testing "blank and whitespace-only hrefs resolve to nil"
    (t/is (nil? (sut/resolve-document-href "docs/x.md" "")))
    (t/is (nil? (sut/resolve-document-href "docs/x.md" "   ")))))
