(ns knoxx.backend.extern.platform-api-catalog
  "Owns public schema asset filesystem/hash/JSON boundaries, not API credentials."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.social-api-codec :as codec]
            ["node:crypto" :as crypto]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

(defonce ^:private cache* (atom {}))

(defn- checksum-file [file] (str/replace file #"\.json$" ".sha256"))

(defn- ^:async decode-file!
  [file]
  (let [bytes (await (.readFile fs file))
        expected (str/trim (await (.readFile fs (checksum-file file) "utf8")))
        actual (-> (.createHash crypto "sha256") (.update bytes) (.digest "hex"))]
    (when-not (and (re-matches #"[a-f0-9]{64}" expected) (= expected actual))
      (throw (ex-info "Platform schema asset checksum mismatch" {:code "platform_catalog_checksum"})))
    {:catalog (js->clj (.parse js/JSON (.toString bytes "utf8"))) :sha256 actual}))

(defn- ^:async cached-file!
  [file]
  (let [stat (await (.stat fs file)) hash-stat (await (.stat fs (checksum-file file)))
        signature [(.-mtimeMs stat) (.-size stat) (.-mtimeMs hash-stat)]
        cached (get @cache* file)]
    (if (= signature (:signature cached))
      (await (:promise cached))
      (let [pending (decode-file! file)]
        (swap! cache* assoc file {:signature signature :promise pending})
        (try (await pending)
             (catch :default error
               (swap! cache* #(if (identical? pending (get-in % [file :promise])) (dissoc % file) %))
               (throw error)))))))

(defn ^:async read!
  "Read a checksum-verified catalog from configured resource roots. Never fetch schemas live."
  [roots]
  (loop [[root & remaining] roots]
    (when-not root
      (throw (ex-info "Pinned platform catalog is missing; run scripts/build-social-api-catalog.mjs"
                      {:code "platform_catalog_missing"})))
    (let [file (.join path root ".social-api" "catalog.json")
          attempt (try {:value (await (cached-file! file))}
                       (catch :default error
                         (if (= "ENOENT" (.-code error)) {:missing true} (throw error))))]
      (if (:missing attempt) (recur remaining) (:value attempt)))))

(defn render
  "Render schema/API data without losing dynamically named JSON fields."
  [value]
  (let [view (if (:requestGuide value)
               (-> (select-keys value [:operation :requestGuide :source :catalogSha256])
                   (assoc :definitionAccess "Complete unexpanded definition remains in structured details. Use social.describe with reference for a focused public definition."))
               value)
        rendered (if (:requestGuide value)
                   (let [text (codec/encode view) clipped? (> (count text) 20000)]
                     {:text (if clipped? (subs text 0 20000) text) :truncated clipped?})
                   (codec/render view))]
    (cond-> rendered (:truncated rendered)
      (update :text str " omitted fields are not absent fields; request a specific local schema reference."))))
