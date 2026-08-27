(ns knoxx.backend.law.publication-manifest
  "Contracts and pure transforms for the published-content manifest.

   The manifest is the published fact: a file on disk that no manifest entry
   names is not public. This namespace owns the manifest's SHAPE and every
   pure decision about it — route derivation, upsert/removal keyed by
   `:publication/id`, EDN round-trip — so the filesystem adapter only
   transports. No I/O lives here.

   The cross-repo contract this declares the writer side of is
   `docs/notes/published-content-manifest-cross-repo-contract.md` in Foresight:
   EDN with namespaced keys, `manifest.edn` at the content root, artifacts
   under `artifacts/<document>/<locale>/<revision>.<ext>`, and an absent
   manifest meaning an empty published set rather than an error."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [knoxx.backend.law.publication-locale :as locale]
            [malli.core :as m]
            [malli.error :as me]))

;; ── Route and manifest contracts ────────────────────────────────────────────

(defn nonblank-string?
  "True for a string carrying at least one non-whitespace character."
  [value]
  (and (string? value) (seq (str/trim value))))

(defn valid-route-path?
  "A public route path: absolute, no query or fragment. Mirrors
    `law.publication/valid-publication-path?`; restated here because that
    namespace is `.cljs` (its bytes predicate has no portable spelling) and
    this contract must stay portable."
  [value]
  (boolean
   (and (nonblank-string? value)
        (str/starts-with? value "/")
        (not (str/includes? value "?"))
        (not (str/includes? value "#"))
        (not (str/includes? value "\u0000")))))

(defn valid-artifact-path?
  "A manifest-relative artifact path: relative, no traversal, no query or
    fragment. What makes a route servable is that this path resolves inside
    the content root, so anything that could escape it is refused at the
    contract rather than at the filesystem."
  [value]
  (boolean
   (and (nonblank-string? value)
        (not (str/starts-with? value "/"))
        (not (str/includes? value ".."))
        (not (str/includes? value "\\"))
        (not (str/includes? value "?"))
        (not (str/includes? value "#"))
        (not (str/includes? value "\u0000")))))

(defn valid-media-type?
  "`type/subtype` with no parameters. Mirrors
    `law.publication/valid-media-type?` — the media type on a route IS the
    artifact's media type, carried verbatim, so the two predicates must accept
    exactly the same values."
  [value]
  (boolean
   (and (string? value)
        (re-matches #"[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]*/[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]*"
                    value))))

(def ManifestRoute
  "One published route. The required set is what the cross-repo contract makes
    a reader fail loudly without (`:route/path`, `:route/locale`,
    `:route/artifact`, `:route/media-type`) plus what THIS writer always emits
    and its own `observe!` depends on: `:route/revision`, `:route/encoding`,
    and `:publication/id`. `:route/artifact` is a PATH, never the artifact
    value — the manifest names where bytes live; it never holds them."
  [:map
   [:route/path [:and string? [:fn valid-route-path?]]]
   [:route/locale locale/Locale]
   [:route/artifact [:and string? [:fn valid-artifact-path?]]]
   [:route/media-type [:and string? [:fn valid-media-type?]]]
   [:route/encoding [:fn nonblank-string?]]
   [:route/revision [:fn nonblank-string?]]
   [:publication/id [:fn qualified-keyword?]]
   [:route/document {:optional true} [:fn nonblank-string?]]
   [:route/title {:optional true} string?]])

(def Manifest
  "The published fact. `:manifest/version` lets a reader reject a shape it
    does not understand rather than rendering a blank page."
  [:map
   [:manifest/version [:= 1]]
   [:manifest/generated-at [:fn nonblank-string?]]
   [:manifest/routes [:vector ManifestRoute]]])

(defn assert-manifest!
  "Return `manifest` when it satisfies the contract; otherwise throw. Called
    on every write and on every read-back: a malformed manifest is a writer
    defect, and continuing past it would publish routes nobody can account
    for."
  [manifest]
  (if (m/validate Manifest manifest)
    manifest
    (throw
     (ex-info "Published content manifest contract violation"
              {:contract :publication/manifest
               :errors (me/humanize (m/explain Manifest manifest))}))))

;; ── Lifecycle ───────────────────────────────────────────────────────────────

(defn- now-iso
  "The current instant as an ISO-8601 string."
  []
  #?(:cljs (.toISOString (js/Date.))
     :clj (str (java.time.Instant/now))))

(defn empty-manifest
  "A manifest with no routes. An empty content root is a valid initial state,
    and this is its manifest: not an error, not a missing file the reader must
    special-case beyond \"nothing published yet\"."
  []
  {:manifest/version 1
   :manifest/generated-at (now-iso)
   :manifest/routes []})

(defn touch
  "Return `manifest` with `:manifest/generated-at` set to now. The timestamp
    is diagnostic, never load-bearing: no convergence decision reads it."
  [manifest]
  (assoc manifest :manifest/generated-at (now-iso)))

;; ── Path derivation ─────────────────────────────────────────────────────────

(defn sanitize-segment
  "Map an arbitrary string to one safe filesystem path segment.

    Every character outside `[A-Za-z0-9._-]` becomes `-`, leading dots are
    stripped (dotfiles and traversal), and an empty result falls back to
    `\"-\"`. Inputs here are contract-checked upstream — a concrete revision is
    a non-blank string, a locale a language-tag keyword — but the segment is
    what touches a filesystem, so the check is repeated at the derivation
    rather than trusted."
  [value]
  (let [cleaned (-> (str value)
                    (str/replace #"[^A-Za-z0-9._-]+" "-")
                    (str/replace #"^\.+" ""))]
    (if (seq cleaned) cleaned "-")))

(defn document-path-segment
  "The document's manifest path segment(s): `namespace/name` for a qualified
    keyword, `name` otherwise, each side sanitized."
  [document-id]
  (if (namespace document-id)
    (str (sanitize-segment (namespace document-id))
         "/"
         (sanitize-segment (name document-id)))
    (sanitize-segment (name document-id))))

(def ^:private media-type-extension-aliases
  "Conventional extensions where the bare subtype is not the name a person
   browsing the content root expects. Naming only — `:route/media-type` is
   the serving truth and is never derived from the extension."
  {"text/plain" "txt"
   "text/markdown" "md"
   "image/jpeg" "jpg"})

(defn media-type-extension
  "The file extension for a media type: a conventional alias when one exists,
    else the subtype up to any `+` suffix, sanitized. `text/html` -> `html`,
    `image/svg+xml` -> `svg`. This is a NAMING convenience only."
  [media-type]
  (or (get media-type-extension-aliases media-type)
      (-> (subs media-type (inc (str/index-of media-type "/")))
          (str/replace #"\+.*$" "")
          sanitize-segment)))

(defn artifact-relative-path
  "Where the bytes live, relative to the content root:
    `artifacts/<document>/<locale>/<revision>.<ext>`.

    Derived from the OP'S INTENT (document and locale) and the validated
    artifact (revision and media type) — deliberately not from
    `:artifact/locale`. The effect boundary has already established that
    intent and artifact agree before an adapter runs, so the two are equal
    here; deriving from intent keeps the route's file layout owned by the
    publication that asked for it."
  [intent artifact]
  (str "artifacts/"
       (document-path-segment (:publication/document intent)) "/"
       (sanitize-segment (name (:publication/locale intent))) "/"
       (sanitize-segment (:artifact/revision artifact)) "."
       (media-type-extension (:artifact/media-type artifact))))

;; ── Route construction and transforms ───────────────────────────────────────

(defn route-for-artifact
  "The manifest route for publishing `artifact` at `intent`.

    `:route/media-type` and `:route/encoding` are copied VERBATIM from the
    validated artifact — no derivation, no defaulting. `:route/artifact` is
    the relative artifact PATH, never the artifact value.

    `:route/title` comes from the hydrated intent's `:document/title`, which
    `law.publication/hydrate-publication-intent` copies off the referenced
    Document after validating it. It is what a reader's listing renders; the
    contract has always declared the key optional and nothing populated it, so
    every published route listed as untitled."
  [intent artifact]
  (cond-> {:route/path (:publication/path intent)
   :route/locale (:publication/locale intent)
   :route/document (document-path-segment (:publication/document intent))
   :route/revision (:artifact/revision artifact)
   :route/artifact (artifact-relative-path intent artifact)
   :route/media-type (:artifact/media-type artifact)
   :route/encoding (:artifact/encoding artifact)
   :publication/id (:publication/id intent)}

    ;; Optional, and omitted rather than blank. A reader's listing falls back to
    ;; the document id when there is no title, which is a worse label than a
    ;; real one and a better one than an empty string.
    (nonblank-string? (:document/title intent))
    (assoc :route/title (:document/title intent))))

(defn find-route
  "The route materialized for `publication-id`, or nil. Keyed on publication
    IDENTITY, not path: after a path move the desired path names nothing yet,
    and the route being replaced is only findable by who published it."
  [manifest publication-id]
  (->> (:manifest/routes manifest)
       (filter #(= publication-id (:publication/id %)))
       first))

(defn upsert-route
  "Return `manifest` with `route` as the one route for its `:publication/id`.

    Replacement, not addition: a path move must leave exactly one public
    route, so any prior route carrying the same publication id is displaced
    wherever its path points."
  [manifest route]
  (update manifest :manifest/routes
          (fn [routes]
            (conj (vec (remove #(= (:publication/id route) (:publication/id %))
                               routes))
                  route))))

(defn remove-route
  "Return `manifest` with no route for `publication-id`. Idempotent: removing
    what was never there is the same manifest."
  [manifest publication-id]
  (update manifest :manifest/routes
          (fn [routes]
            (vec (remove #(= publication-id (:publication/id %)) routes)))))

;; ── EDN round-trip ──────────────────────────────────────────────────────────

(defn manifest->edn
  "Serialize a manifest to EDN. Namespaced keys survive the round-trip, which
    is the reason the cross-repo contract chose EDN over JSON."
  [manifest]
  (pr-str (assert-manifest! manifest)))

(defn edn->manifest
  "Parse and validate a manifest from EDN. Throws on malformed input — a
    corrupt manifest is a writer defect and must fail loudly, never read as
    an empty published set."
  [edn-string]
  (assert-manifest! (edn/read-string edn-string)))
