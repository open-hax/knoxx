(ns knoxx.backend.infra.control-config
  "Thin compatibility shell for the resource-native event runtime control view.

   The runtime model is resources-first: agents, triggers, actions, schedules,
   and generators remain separate tables. This namespace does not synthesize
   agent jobs, does not read adapter-specific defaults, and does not translate
   legacy producer/trigger fields into runnable agent shapes."
  (:require [knoxx.backend.domain.control.catalog :as control-catalog]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.runtime.roles :as roles]))

(def ^:private control-redis-key "events:control-config")

(defn- resource-records
  [config]
  (driver-builtin/register-built-in-drivers!)
  (resources/load-all-resources-sync config))

(defn event-control-config
  "Return the resource-native event runtime control catalog.

   Agents, triggers, actions, schedules, and generators are intentionally kept
   separate. Legacy persisted job overrides are not interpreted here; runtime
   behavior must come from resources that satisfy boundary contracts."
  [config]
  (control-catalog/catalog (resource-records config)))

(defn event-role-options
  [config]
  (roles/list-role-slugs config))

(defn event-generator-kind-options
  [config]
  (control-catalog/generator-kind-options (resource-records config)))

(defn event-trigger-kind-options
  []
  control-catalog/trigger-kind-options)

(defn ^:async persist-event-control!
  "Compatibility persistence hook for old admin clients.

   The new runtime is resource-backed, so this stores only the submitted control
   view for client round-tripping and does not become runtime truth."
  [control]
  (if-let [client (redis/get-client)]
    (try
      (await (redis/set-json client control-redis-key control))
      control
      (catch :default _
        control))
    control))

(defn ^:async load-event-control
  "Load a legacy persisted admin control snapshot, if present. Runtime truth is
   still the resource catalog returned by event-control-config."
  []
  (if-let [client (redis/get-client)]
    (try
      (await (redis/get-json client control-redis-key))
      (catch :default _
        nil))
    nil))
