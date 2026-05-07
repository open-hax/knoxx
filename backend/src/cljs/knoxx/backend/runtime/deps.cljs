(ns knoxx.backend.runtime.deps
  "Compatibility runtime dependency bag.

   Older route/tool namespaces still receive selected host dependencies through a
   JS runtime object. New code should prefer requiring the Node/npm module in the
   namespace that consumes it, then remove that key from this bag."
  (:require ["@open-hax/eta-mu" :as sdk]
            ["node:crypto" :as crypto]
            ["node:fs" :as fs]
            ["node:fs/promises" :as fs-promises]
            ["node:path" :as path]
            ["node:os" :as os]
            ["node:child_process" :refer [execFile]]
            ["node:util" :refer [promisify]]))

(defn- fs-bundle
  "Knoxx mostly wants fs/promises, but media routes also need createReadStream."
  []
  (js/Object.assign
   #js {}
   fs-promises
   #js {:createReadStream (aget fs "createReadStream")
        :promises (aget fs "promises")}))

(defn make-runtime
  "Build the runtime object that still bridges older route code."
  [policyDb]
  #js {:sdk sdk
       :crypto crypto
       :fs (fs-bundle)
       :path path
       :os os
       :execFileAsync (promisify execFile)
       :policyDb policyDb})
