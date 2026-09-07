(ns open-hax.publication-wire
  "The publication wire vocabulary, shared by the backend and the frontend.

  This namespace exists so a wire key and its validator cannot drift apart. It is
  consumed from both sides of the boundary:

  - the backend builds its Malli contracts from these keys and enum values
  - the frontend builds its request bodies from the same values

  A rename here breaks both sides at once, which is the point. Nothing here
  depends on Malli, because the frontend build has no Malli — the *vocabulary* is
  shared, and each side derives its own validation or construction from it.

  Wire keys are deliberately **unqualified**. `knoxx.frontend.lib.api/request`
  serializes with `clj->js`, which drops keyword namespaces, so a qualified wire
  key can never survive a real request."
  (:require [clojure.string :as str]))

;; ── Publication state ──────────────────────────────────────────────────────

(def state-patch-key
  "The single key in a publication state PATCH body."
  :state)

(def state-values
  "Domain state keyword -> wire string. The full set of states a CMS edit may
   request."
  {:draft "draft"
   :published "published"
   :withheld "withheld"
   :archived "archived"})

(def state-wire-values
  (vec (sort (vals state-values))))

(defn encode-state
  [state]
  (get state-values state))

(defn decode-state
  [wire-state]
  (get (into {} (map (fn [[k v]] [v k])) state-values) wire-state))

(defn state-patch-body
  "The exact body a CMS state edit sends. Built here so the frontend helper and
   the backend contract are literally the same construction."
  [state]
  {state-patch-key (encode-state state)})

;; ── Garden status ──────────────────────────────────────────────────────────

(def garden-status-values
  {:active "active" :archived "archived"})

(def garden-status-wire-values
  (vec (sort (vals garden-status-values))))

;; ── Revision selectors ─────────────────────────────────────────────────────

(def revision-selector-tokens
  "Revision selectors that cross the wire as their own token.

   Decoding cannot simply keywordize: `\"abc123\"` is a concrete revision and must
   stay a string, while `\"source/current\"` is a selector and must decode back to
   `:source/current`. Only a known token list can tell them apart."
  {"source/current" :source/current})

(defn encode-revision
  [revision]
  (if (keyword? revision)
    (if-let [ns-part (namespace revision)]
      (str ns-part "/" (name revision))
      (name revision))
    revision))

(defn decode-revision
  [wire-revision]
  (get revision-selector-tokens wire-revision wire-revision))

;; ── Row key sets ───────────────────────────────────────────────────────────

(def document-keys [:id :title :source-locale :source])
(def garden-keys [:id :title :status])
(def publication-keys [:id :document :garden :locale :revision :path
                       :desired :observed :blockers])
(def document-view-keys [:document :publications])
(def list-view-keys [:documents :gardens])

;; ── Identity ───────────────────────────────────────────────────────────────

(defn encode-id
  "`:docs/probe` -> `\"docs/probe\"`. Never `(str keyword)`, which emits the EDN
   leading colon and would produce a spurious `%3A` in a URL."
  [id]
  (if (keyword? id)
    (if-let [ns-part (namespace id)]
      (str ns-part "/" (name id))
      (name id))
    (str id)))

(defn decode-id
  "`\"docs/probe\"` -> `:docs/probe`."
  [wire-id]
  (cond
    (keyword? wire-id) wire-id
    (not (string? wire-id)) wire-id
    (str/includes? wire-id "/") (let [[ns-part name-part] (str/split wire-id #"/" 2)]
                                  (keyword ns-part name-part))
    :else (keyword wire-id)))
