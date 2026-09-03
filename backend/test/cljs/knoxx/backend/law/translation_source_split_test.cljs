(ns knoxx.backend.law.translation-source-split-test
  (:require [cljs.test :as t]
            [clojure.string :as str]
            [knoxx.backend.law.translation-source-split :as source-split]))

(defn- assert-exact-partition!
  "Assert the public partition laws for one nonblank source."
  [source]
  (let [parts (source-split/source-parts source)]
    (t/is (vector? parts))
    (t/is (seq parts))
    (t/is (every? (complement str/blank?) parts))
    (t/is (= source (apply str parts)))
    ;; These are exactly the source-part preconditions split-manifest admits.
    (t/is (source-split/exact-source-parts? source parts))))

(t/deftest separators-trail-the-preceding-paragraph
  (let [source "# Heading\n\nFirst paragraph.\n\n\nSecond paragraph.\n"]
    (t/is (= ["# Heading\n\n"
              "First paragraph.\n\n\n"
              "Second paragraph.\n"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest crlf-and-blank-line-padding-remain-exact
  (let [source "First\r\n \t\r\nSecond\r\n\r\nThird"]
    (t/is (= ["First\r\n \t\r\n"
              "Second\r\n\r\n"
              "Third"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest leading-and-trailing-whitespace-never-become-phantom-parts
  (let [source " \r\n\r\n  First paragraph.\n\nSecond.\n\n \t"]
    (t/is (= [" \r\n\r\n  First paragraph.\n\n"
              "Second.\n\n \t"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest a-single-line-break-is-not-a-paragraph-boundary
  (let [source "one\r\ntwo\nthree"]
    (t/is (= [source] (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest unicode-and-mixed-line-endings-are-not-normalized
  (let [source "🙂 alpha\r\n\nβeta\n\r最後"]
    (t/is (= ["🙂 alpha\r\n\n"
              "βeta\n\r"
              "最後"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest fenced-code-blocks-are-indivisible
  (doseq [source ["Before\n\n```clojure\n(+ 1 2)\n\n(+ 3 4)\n```\n\nAfter"
                  "Before\r\n\r\n  ~~~ text\r\ncode\r\n\r\nmore\r\n  ~~~\r\n\r\nAfter"]]
    (let [parts (source-split/source-parts source)]
      (t/is (= 3 (count parts)))
      (assert-exact-partition! source))))

(t/deftest shorter-inner-runs-do-not-close-longer-fences
  (let [source "````markdown\nalpha\n\n```\nbeta\n\n````\n\nAfter"]
    (t/is (= ["````markdown\nalpha\n\n```\nbeta\n\n````\n\n" "After"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest unclosed-fences-protect-the-rest-of-the-source
  (let [source "Before\n\n```\ncode\n\nstill code\n\nnot prose"]
    (t/is (= ["Before\n\n" "```\ncode\n\nstill code\n\nnot prose"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest closed-mapping-front-matter-keeps-internal-blank-lines
  (let [source "---\ntitle: Knoxx\n\ndescription: Review\n---\n\n# Heading\n\nBody"]
    (t/is (= ["---\ntitle: Knoxx\n\ndescription: Review\n---\n\n"
              "# Heading\n\n"
              "Body"]
             (source-split/source-parts source)))
    (assert-exact-partition! source))

  (let [source (str "\uFEFF+++\r\ntitle = \"Knoxx\"\r\n\r\n"
                    "[review]\r\nowner = \"knowledge-ops\"\r\n+++\r\n\r\nBody")]
    (t/is (= [(str "\uFEFF+++\r\ntitle = \"Knoxx\"\r\n\r\n"
                   "[review]\r\nowner = \"knowledge-ops\"\r\n+++\r\n\r\n")
              "Body"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest thematic-rules-and-unclosed-front-matter-do-not-swallow-prose
  (let [source "---\nOpening prose.\n\n---\n\nNext"]
    (t/is (= ["---\nOpening prose.\n\n" "---\n\n" "Next"]
             (source-split/source-parts source)))
    (assert-exact-partition! source))

  (let [source "---\ntitle: Knoxx\n\nBody without a closing marker"]
    (t/is (= ["---\ntitle: Knoxx\n\n" "Body without a closing marker"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest indented-code-blocks-retain-only-their-internal-blank-lines
  (let [source "Before\n\n    alpha\n\n    beta\n\nAfter"]
    (t/is (= ["Before\n\n"
              "    alpha\n\n    beta\n\n"
              "After"]
             (source-split/source-parts source)))
    (assert-exact-partition! source))

  (let [source "Before\r\n\r\n\talpha\r\n \t\r\n\tbeta\r\n\r\nAfter"]
    (t/is (= ["Before\r\n\r\n"
              "\talpha\r\n \t\r\n\tbeta\r\n\r\n"
              "After"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest indentation-after-prose-cannot-retroactively-open-a-code-block
  (let [source "Intro\n    continuation\n\n    code\n\nNext"]
    (t/is (= ["Intro\n    continuation\n\n"
              "    code\n\n"
              "Next"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest indented-code-may-start-after-a-complete-nonparagraph-block
  (doseq [source ["```\nx\n```\n    code a\n\n    code b\n\nAfter"
                  "# Heading\n    code a\n\n    code b\n\nAfter"
                  "***\n    code a\n\n    code b\n\nAfter"]]
    (let [parts (source-split/source-parts source)]
      (t/is (= 2 (count parts)))
      (t/is (str/includes? (first parts) "code a\n\n    code b"))
      (t/is (= "After" (second parts)))
      (assert-exact-partition! source))))

(t/deftest list-item-subtrees-retain-context-without-merging-siblings
  (let [source "- First.\n\n  More first.\n\n- Second.\n\nAfter"]
    (t/is (= ["- First.\n\n  More first.\n\n"
              "- Second.\n\n"
              "After"]
             (source-split/source-parts source)))
    (assert-exact-partition! source))

  (let [source (str "- Parent\n\n  - Child A\n\n    Child continuation\n\n"
                    "  - Child B\n\n- Sibling")]
    (t/is (= [(str "- Parent\n\n  - Child A\n\n    Child continuation\n\n"
                   "  - Child B\n\n")
              "- Sibling"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest ordered-list-width-changes-and-invalid-markers-stay-boundaries
  (let [source "9. Nine\n\n10. Ten\n\nAfter"]
    (t/is (= ["9. Nine\n\n" "10. Ten\n\n" "After"]
             (source-split/source-parts source)))
    (assert-exact-partition! source))

  (doseq [source ["1.not a list\n\nParagraph"
                  "---\nA horizontal rule, not metadata.\n\nParagraph"]]
    (t/is (= 2 (count (source-split/source-parts source))))
    (assert-exact-partition! source)))

(t/deftest lazy-list-continuations-remain-in-their-own-item-subtree
  (let [source "- first\nlazy continuation\n\n  second paragraph\n\n- sibling"]
    (t/is (= ["- first\nlazy continuation\n\n  second paragraph\n\n"
              "- sibling"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest context-free-markers-cannot-swallow-following-indentation
  (doseq [[source expected]
          [["Intro\n2. ordinary text\n\n   separate paragraph\n\nAfter"
            ["Intro\n2. ordinary text\n\n"
             "   separate paragraph\n\n"
             "After"]]
           ["Intro\n-\n\n  separate\n\nAfter"
            ["Intro\n-\n\n" "  separate\n\n" "After"]]
           ["Intro\n1.\n\n   separate\n\nAfter"
            ["Intro\n1.\n\n" "   separate\n\n" "After"]]]]
    (t/is (= expected (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest paragraph-interrupting-blocks-end-a-list-item-subtree
  (doseq [[source first-part]
          [["- item\n***\n\n  separate paragraph\n\nAfter"
            "- item\n***\n\n"]
           ["- item\n> quote\n\n  separate paragraph\n\nAfter"
            "- item\n> quote\n\n"]]]
    (let [parts (source-split/source-parts source)]
      (t/is (= [first-part "  separate paragraph\n\n" "After"] parts))
      (assert-exact-partition! source))))

(t/deftest list-contained-tables-keep-the-parent-item-context
  (let [source (str "- Metrics:\n\n"
                    "  | Name | Notes |\n"
                    "  | :--- | ---: |\n"
                    "  | a\\|b | `x | y` |\n\n"
                    "- Next\n\nOutside")]
    (t/is (= [(str "- Metrics:\n\n"
                   "  | Name | Notes |\n"
                   "  | :--- | ---: |\n"
                   "  | a\\|b | `x | y` |\n\n")
              "- Next\n\n"
              "Outside"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest standalone-tables-and-pipe-prose-remain-ordinary-blocks
  (let [source (str "| Name | Notes |\n"
                    "| :--- | ---: |\n"
                    "| a\\|b | `x | y` |\n\n"
                    "Paragraph\n\n"
                    "| Other | Value |\n"
                    "| --- | --- |\n"
                    "| c | d |")]
    (t/is (= [(str "| Name | Notes |\n"
                   "| :--- | ---: |\n"
                   "| a\\|b | `x | y` |\n\n")
              "Paragraph\n\n"
              (str "| Other | Value |\n"
                   "| --- | --- |\n"
                   "| c | d |")]
             (source-split/source-parts source)))
    (assert-exact-partition! source))

  (let [source "A | B\n\nC | D"]
    (t/is (= ["A | B\n\n" "C | D"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest validated-table-can-precede-one-indented-code-block
  (let [source (str "| A |\n"
                    "|---|\n"
                    "| B |\n"
                    "    code a\n\n"
                    "    code b\n\n"
                    "After")]
    (t/is (= [(str "| A |\n"
                   "|---|\n"
                   "| B |\n"
                   "    code a\n\n"
                   "    code b\n\n")
              "After"]
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest blank-sources-are-not-admissible-translation-input
  (doseq [source ["" " \t" "\r\n\r\n"]]
    (t/is (thrown-with-msg?
           js/Error
           #"must not be blank"
           (source-split/source-parts source)))
    (t/is (not (source-split/exact-source-parts? source [])))))

(t/deftest source-partitioning-is-deterministic
  (let [source "\n\nFirst\n \nSecond\r\n\r\n"]
    (t/is (= (source-split/source-parts source)
             (source-split/source-parts source)))
    (assert-exact-partition! source)))

(t/deftest invalid-input-and-lossy-parts-are-refused
  (t/is (thrown-with-msg?
         js/Error
         #"must be a string"
         (source-split/source-parts nil)))
  (t/is (not (source-split/exact-source-parts?
              "one\n\ntwo" ["one" "two"])))
  (t/is (not (source-split/exact-source-parts?
              "one\n\ntwo" ["one\n\n" ""])))
  (t/is (not (source-split/exact-source-parts?
              "one\n\ntwo" ["one\n\ntwo"]))))
