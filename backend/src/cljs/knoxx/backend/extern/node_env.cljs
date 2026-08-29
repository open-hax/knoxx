(ns knoxx.backend.extern.node-env
  "The process environment boundary.

  `js/process.env` is a JavaScript object and reading it needs `aget`, which the
  style guide confines to `extern.*`. This adapter owns that read and hands back
  a CLJS scalar, so infrastructure code asking whether a variable is set never
  touches interop to find out.

  Sibling to `extern.node-fs`, which owns `node:fs` for the same reason. It is
  deliberately read-only: nothing in Knoxx should be mutating its own
  environment at runtime, and an adapter that cannot write is a boundary that
  cannot be misused."
  (:require [clojure.string :as str]))

(defn variable
  "The trimmed value of environment variable `name`, or nil.

   Nil for absent, empty and whitespace-only alike. That collapse is deliberate:
   every caller here treats a blank variable as unset — a deployment that writes
   `FOO=` means the same thing as one that omits `FOO`, and a sentinel of spaces
   should never be mistaken for a configured secret."
  [name]
  (some-> (aget js/process.env name) str str/trim not-empty))
