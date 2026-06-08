(ns knoxx.backend.infra.auth.token-hash
  "Session token hashing shared by the PG and Mongo policy stores.

   Extracted from infra.db.policy so both storage backends verify tokens
   with identical semantics: salted sha256 hex for storage, an unsalted
   12-hex-char prefix for candidate lookup, 16 random bytes of salt.
   Documented low-level node:crypto boundary."
  (:require ["node:crypto" :as crypto]))

(defn hash-token
  "Salted sha256 hex digest of a session token for at-rest storage."
  [token salt]
  (let [h (.createHash crypto "sha256")]
    (.update h (str salt ":" token) "utf8")
    (.digest h "hex")))

(defn token-prefix
  "Deterministic 12-hex-char lookup prefix for a token (unsalted)."
  [token]
  (let [h (.createHash crypto "sha256")]
    (.update h (str token) "utf8")
    (subs (.digest h "hex") 0 12)))

(defn generate-salt
  "16 random bytes as hex."
  []
  (.toString (.randomBytes crypto 16) "hex"))

(defn generate-secret
  "32 random bytes as hex — used for the persisted session secret."
  []
  (.toString (.randomBytes crypto 32) "hex"))
