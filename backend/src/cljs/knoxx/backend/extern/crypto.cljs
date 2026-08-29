(ns knoxx.backend.extern.crypto
  "Node crypto boundary for Knoxx-owned hashing and comparison operations."
  (:require ["node:crypto" :as crypto]))

(defn random-hex
  [byte-count]
  (.toString (.randomBytes crypto byte-count) "hex"))

(defn scrypt-hex
  [value salt byte-count]
  (.toString (.scryptSync crypto (str value) (str salt) byte-count) "hex"))

(defn secure-hex=
  [expected actual]
  (let [expected-buffer (.from js/Buffer (str expected) "hex")
        actual-buffer (.from js/Buffer (str actual) "hex")]
    (and (pos? (.-length expected-buffer))
         (= (.-length expected-buffer) (.-length actual-buffer))
         (.timingSafeEqual crypto expected-buffer actual-buffer))))
