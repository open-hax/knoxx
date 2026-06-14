(ns knoxx.frontend.lib.edn
  "EDN parse/serialize for view-contract.edn files.
   Replaces the hand-rolled parser in src/lib/edn.ts with the platform
   reader — unlike the TS parser this preserves namespaced keywords
   (:view/id) and string escapes."
  (:require [cljs.reader :as reader]))

(def ^{:doc "Parses an EDN string into CLJS data."}
  parse-edn reader/read-string)

(def ^{:doc "Serializes CLJS data to a readable EDN string."}
  serialize-edn pr-str)
