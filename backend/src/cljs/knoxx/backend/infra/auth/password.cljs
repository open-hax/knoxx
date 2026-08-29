(ns knoxx.backend.infra.auth.password
  "Password credential encoding and verification for Knoxx-owned local auth."
  (:require [knoxx.backend.extern.crypto :as crypto]))

(defn hash-password
  [password]
  (let [salt (crypto/random-hex 16)
        hash (crypto/scrypt-hex password salt 64)]
    {:algorithm "scrypt"
     :salt salt
     :hash hash}))

(defn valid-password?
  [password secret]
  (let [salt (:salt secret)
        expected (:hash secret)]
    (and (= "scrypt" (:algorithm secret))
         (string? salt)
         (boolean (re-matches #"[0-9a-f]{32}" salt))
         (string? expected)
         (boolean (re-matches #"[0-9a-f]{128}" expected))
         (crypto/secure-hex= expected (crypto/scrypt-hex password salt 64)))))
