(ns knoxx.backend.law.publication-manifest-test
  "The pure half of the static-site target: manifest shape, path derivation,
   and the route transforms the filesystem adapter commits."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.law.publication-manifest :as manifest]))

(def ^:private intent
  {:publication/id :knoxx.docs/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/es/notes/hello"
   :translation/review :required
   :document/source-locale :en
   :document/title "Probe"})

(def ^:private artifact
  {:artifact/content "<!doctype html><p>Hola</p>"
   :artifact/media-type "text/html"
   :artifact/encoding "utf-8"
   :artifact/locale :es
   :artifact/revision "rev-7f3a91c"})

;; ── path derivation ────────────────────────────────────────────────────────

(deftest artifact-path-is-document-locale-revision
  (is (= "artifacts/knoxx.docs/probe/es/rev-7f3a91c.html"
         (manifest/artifact-relative-path intent artifact)))
  (testing "the locale segment comes from the INTENT, not the artifact — the
            boundary established they agree, and the file layout belongs to
            the publication that asked for it"
    (is (= "artifacts/knoxx.docs/probe/es/rev-7f3a91c.html"
           (manifest/artifact-relative-path
            intent (assoc artifact :artifact/locale :es)))))
  (testing "the extension follows the media type's subtype"
    (is (= "artifacts/knoxx.docs/probe/es/rev-7f3a91c.svg"
           (manifest/artifact-relative-path
            intent (assoc artifact :artifact/media-type "image/svg+xml"))))
    (is (= "artifacts/knoxx.docs/probe/es/rev-7f3a91c.txt"
           (manifest/artifact-relative-path
            intent (assoc artifact :artifact/media-type "text/plain"))))))

(deftest path-segments-cannot-escape-the-content-root
  (testing "a revision carrying separators or traversal is flattened into one
            inert segment — contract-checked upstream, re-checked here because
            this string touches a filesystem"
    (is (= "artifacts/knoxx.docs/probe/es/a-b-c.html"
           (manifest/artifact-relative-path
            intent (assoc artifact :artifact/revision "a/b\\c"))))
    (is (= "artifacts/knoxx.docs/probe/es/-rev.html"
           (manifest/artifact-relative-path
            intent (assoc artifact :artifact/revision "../rev"))))
    (is (not (str/includes?
              (manifest/artifact-relative-path
               intent (assoc artifact :artifact/revision "../rev"))
              ".."))
        "no traversal survives, however the dots fall")
    (is (every? (fn [segment]
                  (and (not= ".." segment) (not= "." segment) (seq segment)))
                (-> (manifest/artifact-relative-path
                     intent (assoc artifact :artifact/revision "..."))
                    (str/split #"/"))))))

;; ── routes ─────────────────────────────────────────────────────────────────

(deftest route-carries-the-document-title
  (testing "`:route/title` is what a reader's listing renders. The contract has
            always declared the key and nothing populated it, so every
            published route listed as untitled."
    (is (= "Probe" (:route/title (manifest/route-for-artifact intent artifact)))))

  (testing "omitted rather than blank when the document has no usable title —
            a reader falls back to the document id, which is a worse label than
            a real title and a better one than an empty string"
    (doseq [empty-ish [nil "" "   "]]
      (is (not (contains? (manifest/route-for-artifact
                           (assoc intent :document/title empty-ish) artifact)
                          :route/title))
          (str "a title of " (pr-str empty-ish) " must not reach the manifest"))))

  (testing "a titled route still satisfies the manifest contract"
    (is (some? (manifest/assert-manifest!
                (manifest/upsert-route (manifest/empty-manifest)
                                       (manifest/route-for-artifact intent artifact)))))))

(deftest route-carries-artifact-values-verbatim
  (let [route (manifest/route-for-artifact intent artifact)]
    (is (= "/es/notes/hello" (:route/path route)))
    (is (= :es (:route/locale route)))
    (is (= "rev-7f3a91c" (:route/revision route)))
    (is (= "text/html" (:route/media-type route)))
    (is (= "utf-8" (:route/encoding route)))
    (is (= :knoxx.docs/probe-es (:publication/id route)))
    (testing "`:route/artifact` is a PATH — never the artifact value the
              memory target stores"
      (is (= "artifacts/knoxx.docs/probe/es/rev-7f3a91c.html"
             (:route/artifact route))))))

(deftest upsert-replaces-by-publication-id
  (let [route (manifest/route-for-artifact intent artifact)
        manifest (-> (manifest/empty-manifest)
                     (manifest/upsert-route route))]
    (is (= 1 (count (:manifest/routes manifest))))
    (testing "a path move leaves exactly one route: replacement is keyed on
              publication identity, so the old path is displaced wherever it
              points"
      (let [moved (assoc route :route/path "/es/notes/moved")
            updated (manifest/upsert-route manifest moved)]
        (is (= 1 (count (:manifest/routes updated))))
        (is (= "/es/notes/moved"
               (:route/path (first (:manifest/routes updated)))))))
    (testing "a different publication id is a different route"
      (let [other (assoc route :publication/id :knoxx.docs/other-es)
            updated (manifest/upsert-route manifest other)]
        (is (= 2 (count (:manifest/routes updated))))))))

(deftest find-and-remove-are-keyed-on-publication-id
  (let [route (manifest/route-for-artifact intent artifact)
        manifest (manifest/upsert-route (manifest/empty-manifest) route)]
    (is (= route (manifest/find-route manifest :knoxx.docs/probe-es)))
    (testing "found by identity even after the path moved"
      (is (= (assoc route :route/path "/moved")
             (manifest/find-route
              (manifest/upsert-route manifest (assoc route :route/path "/moved"))
              :knoxx.docs/probe-es))))
    (is (nil? (manifest/find-route manifest :knoxx.docs/never-published)))
    (is (empty? (:manifest/routes
                 (manifest/remove-route manifest :knoxx.docs/probe-es))))
    (testing "removal is idempotent"
      (is (= (manifest/remove-route manifest :knoxx.docs/never-published)
             manifest)))))

;; ── contract and EDN round-trip ────────────────────────────────────────────

(deftest the-empty-manifest-is-valid
  (is (map? (manifest/assert-manifest! (manifest/empty-manifest))))
  (is (= [] (:manifest/routes (manifest/empty-manifest)))))

(deftest manifest-edn-round-trips-with-namespaced-keys-intact
  (let [route (manifest/route-for-artifact intent artifact)
        written (-> (manifest/empty-manifest)
                    (manifest/upsert-route route)
                    (manifest/touch))
        read-back (manifest/edn->manifest (manifest/manifest->edn written))]
    (is (= written read-back))
    (testing "qualified keywords survive — the reason the contract chose EDN
              over JSON in the first place"
      (is (= :knoxx.docs/probe-es
             (:publication/id (first (:manifest/routes read-back))))))))

(deftest malformed-manifests-fail-loudly
  (doseq [[label bad] [["no version" {:manifest/routes []}]
                       ["unsupported version" {:manifest/version 2
                                               :manifest/generated-at "t"
                                               :manifest/routes []}]
                       ["route without an artifact path"
                        {:manifest/version 1
                         :manifest/generated-at "t"
                         :manifest/routes [{:route/path "/x"
                                            :route/locale :es
                                            :route/media-type "text/html"
                                            :route/encoding "utf-8"
                                            :route/revision "r"
                                            :publication/id :a/b}]}]
                       ["route with a traversal artifact path"
                        {:manifest/version 1
                         :manifest/generated-at "t"
                         :manifest/routes [{:route/path "/x"
                                            :route/locale :es
                                            :route/artifact "../escape"
                                            :route/media-type "text/html"
                                            :route/encoding "utf-8"
                                            :route/revision "r"
                                            :publication/id :a/b}]}]
                       ["route with no publication id"
                        {:manifest/version 1
                         :manifest/generated-at "t"
                         :manifest/routes [{:route/path "/x"
                                            :route/locale :es
                                            :route/artifact "artifacts/a"
                                            :route/media-type "text/html"
                                            :route/encoding "utf-8"
                                            :route/revision "r"}]}]]]
    (testing label
      (is (thrown? js/Error (manifest/assert-manifest! bad)))))
  (testing "unparseable EDN throws rather than reading as empty"
    (is (thrown? js/Error (manifest/edn->manifest "{:manifest/version"))))
  (testing "a reader rule the writer shares: garbage is a defect, not an
            empty published set"
    (is (thrown? js/Error (manifest/edn->manifest "42")))))
