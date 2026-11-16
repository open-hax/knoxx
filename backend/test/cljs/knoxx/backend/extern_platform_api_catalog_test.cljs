(ns knoxx.backend.extern-platform-api-catalog-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.platform-api-catalog :as sut]
            ["node:crypto" :as crypto]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]))

(defn- ^:async write-catalog!
  [root marker]
  (let [dir (.join path root ".social-api")
        bytes (.stringify js/JSON #js {:schemaVersion 1 :marker marker :operations #js [] :documents #js {} :sources #js {}})
        digest (-> (.createHash crypto "sha256") (.update bytes) (.digest "hex"))]
    (await (.mkdir fs dir #js {:recursive true}))
    (await (.writeFile fs (.join path dir "catalog.json") bytes))
    (await (.writeFile fs (.join path dir "catalog.sha256") digest))
    digest))

(deftest ^:async public-assets-are-verified-and-hot-reloaded
  (let [root (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-api-catalog-")))]
    (try
      (let [hash (await (write-catalog! root "first"))
            read (await (sut/read! [(str root "/absent") root]))]
        (is (= hash (:sha256 read)))
        (is (= "first" (get (:catalog read) "marker"))))
      (await (write-catalog! root "a changed catalog"))
      (is (= "a changed catalog" (get (:catalog (await (sut/read! [root]))) "marker")))
      (await (.writeFile fs (.join path root ".social-api/catalog.json") "corrupt content"))
      (try (await (sut/read! [root])) (is false "corrupt public schema must not be accepted")
           (catch :default error (is (= "platform_catalog_checksum" (:code (ex-data error))))))
      (await (write-catalog! root "repaired"))
      (is (= "repaired" (get (:catalog (await (sut/read! [root]))) "marker")))
      (finally (await (.rm fs root #js {:recursive true :force true}))))))

(deftest ^:async missing-catalogs-and-partial-rendering-are-explicit
  (try (await (sut/read! [])) (is false "missing data must not become an empty feature set")
       (catch :default error (is (= "platform_catalog_missing" (:code (ex-data error))))))
  (let [view (sut/render {:large (apply str (repeat 25000 "x"))})]
    (is (true? (:truncated view)))
    (is (.includes (:text view) "omitted fields are not absent fields"))))
