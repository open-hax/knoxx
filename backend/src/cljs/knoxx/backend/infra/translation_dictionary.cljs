(ns knoxx.backend.infra.translation-dictionary
  "Read-only runtime API for the current approved translation dictionary."
  (:require [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-dictionary :as dictionary]))

(defn ^:async current!
  "Read effective approved examples once and project an exact scoped dictionary.

  The split store owns review-history reduction and current-set admission. This
  adapter deliberately has no write operation: dictionary state is rebuilt from
  immutable candidate and review facts on every read."
  [store scope]
  (let [checked (dictionary/checked-scope scope)
        examples (await (split-store/applicable-memory! store checked))]
    (dictionary/projection checked examples)))
