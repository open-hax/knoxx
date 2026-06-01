;; knoxx.backend.domain.node.crypto
;;
;; THE JS BOUNDARY FOR CRYPTO OPS.
;; node:crypto lives HERE AND NOWHERE ELSE.

(ns knoxx.backend.domain.node.crypto
  (:refer-clojure :exclude [random-uuid])
  (:require ["node:crypto" :as node-crypto]))

(defn random-hex
  "n random bytes as lowercase hex string."
  [n]
  (.toString (.randomBytes node-crypto n) "hex"))

(defn random-uuid
  "RFC4122 v4 UUID string."
  []
  (.randomUUID node-crypto))

(defn sha256-hex
  "SHA-256 digest of string as lowercase hex."
  [s]
  (-> (.createHash node-crypto "sha256")
      (.update (str s))
      (.digest "hex")))

(defn md5-hex
  "MD5 digest of string as lowercase hex. Do not use for security."
  [s]
  (-> (.createHash node-crypto "md5")
      (.update (str s))
      (.digest "hex")))
