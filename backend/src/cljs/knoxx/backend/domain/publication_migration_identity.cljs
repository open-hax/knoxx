(ns knoxx.backend.domain.publication-migration-identity
  "Generated identities for migrated publication resources.

  Identity is derived from the legacy relation — document, garden, locale,
  revision — never from a source path or title, so a retitled or moved document
  keeps its publication identity across reruns.

  Two properties matter and both are enforced here rather than assumed:
  identities are *faithful* (a legacy value that cannot be represented is
  rejected instead of being stringified into an id that never existed) and
  *injective* (two distinct relations cannot collapse onto one id)."
  (:require [clojure.string :as str]
            [knoxx.backend.shape.resource-identity :as identity]))


(def id-component-separator
  "Separator for generated identity components. A component containing it is
   rejected, which is what makes the concatenation injective: with `-` as the
   separator, document `a-b` + garden `c` and document `a` + garden `b-c` both
   produced `a-b-c-en`, so two distinct relations collapsed onto one id and the
   second was silently classified as a noop."
  "~")

(defn legacy-name
  "Name component of a legacy identity.

   Only strings and keywords are accepted. Stringifying a number, a map, or nil
   would invent an identity that never existed in the legacy data, and that
   invented id would then be written as a resource."
  [value]
  (cond
    (keyword? value) (not-empty (name value))
    (string? value) (not-empty (str/trim value))
    :else nil))

(defn id-component
  "One component of a generated identity, or nil when it cannot be represented
   faithfully."
  [value]
  (when-let [name-part (legacy-name value)]
    (when-not (str/includes? name-part id-component-separator)
      name-part)))

(defn revision-component
  "Revision as an identity component, tagged so a `:source/current` selector and
   a concrete revision string can never collide."
  [revision]
  (cond
    (= :source/current revision) "sel-current"
    (keyword? revision) (some->> (id-component revision) (str "sel-"))
    :else (some->> (id-component revision) (str "rev-"))))

(defn canonical-garden-id
  [policy row]
  (when-let [garden (id-component (:garden-id row))]
    (identity/canonical-id (:migration/namespace policy) (keyword garden))))

(defn canonical-publication-id
  "Identity is derived from the full relation — document, garden, locale, and
   revision — never from the source path or title, so a retitled or moved
   document keeps its publication identity across reruns.

   Revision is part of it because the resolver's relation key includes revision:
   two valid rows differing only by revision are distinct relations, and an id
   that stopped at locale would collapse them."
  [policy document row revision]
  (let [components [(id-component (:document/id document))
                    (id-component (:garden-id row))
                    (id-component (:locale row))
                    (revision-component revision)]]
    (when (every? some? components)
      (identity/canonical-id (:migration/namespace policy)
                             (keyword (str/join id-component-separator components))))))
