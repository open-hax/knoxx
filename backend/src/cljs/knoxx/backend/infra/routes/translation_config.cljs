(ns knoxx.backend.infra.routes.translation-config
  "Knoxx-owned translation configuration facade.

  The single read boundary for translation model and review policy, consumed by
  the review UI and by the JVM ingestion worker alike, so no consumer resolves
  model precedence for itself. Returns and accepts CLJS data; Fastify handles
  stay in `knoxx.backend.extern.fastify.translation-config`."
  (:require [knoxx.backend.domain.translation-config :as translation-config]
            [knoxx.backend.domain.resources.loader :as resources]))

(def decode-patch
  "Re-exported so the extern adapter decodes through the domain contract rather
   than reaching for the wire schema itself."
  translation-config/decode-config-patch)

(defn ^:async config-records!
  [config]
  (filterv :ok? (await (resources/load-all-resource-records! config))))

(defn global-config-record
  "The record owning the global config, kept alongside the index because the
   file path lives on the record while resolution reads the definition."
  [records]
  (some (fn [record]
          (let [definition (:resource/definition record)]
            (when (and (translation-config/config-resource? definition)
                       (= translation-config/global-config-id
                          (translation-config/config-id definition)))
              record)))
        records))

(defn ^:async config-index!
  [config]
  (->> (await (config-records! config))
       (mapv :resource/definition)
       translation-config/index-resources))

(defn ^:async resolved-config!
  "The resolved, catalog-validated configuration for a request context."
  [config context]
  (translation-config/resolve-config (await (config-index! config)) context))

(defn ^:async config-response!
  "Wire-encoded configuration, as both the UI and the worker consume it."
  [config context]
  (translation-config/config->wire (await (resolved-config! config context))))

(defn ^:async patch-config!
  "Apply a decoded domain patch and persist it.

   The write is a whole-file rewrite of the owning manifest, so the resource
   stays the single authority rather than accumulating a shadow override.

   Refused before the write when the caller's own org override would shadow the
   result, so a patch never reports a change the caller cannot observe."
  [config context domain-patch]
  (let [records (await (config-records! config))
        index (translation-config/index-resources (mapv :resource/definition records))
        patched (translation-config/apply-patch index context domain-patch)
        _ (translation-config/assert-patch-target-is-effective! index context)
        record (global-config-record records)
        file-path (:resource/file-path record)]
    (when-not file-path
      (throw (ex-info "translation config resource has no file on disk"
                      {:expected translation-config/global-config-id})))
    (await (resources/write-edn-file!
            file-path
            (translation-config/manifest-edn-text
             (translation-config/config-resource index patched))))
    (resources/invalidate-sync-resource-cache!)
    (translation-config/config->wire patched)))
