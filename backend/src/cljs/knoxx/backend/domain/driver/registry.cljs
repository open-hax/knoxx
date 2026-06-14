(ns knoxx.backend.domain.driver.registry
  "Delegates to the contract-runtime driver registry.
   Re-exports all public vars for backward compatibility.
   Protocol and record types must be used from open-hax.contract-runtime.driver.registry."
  (:require [open-hax.contract-runtime.driver.registry :as core]))

;; Protocol functions
(def driver-id core/driver-id)
(def driver-kind core/driver-kind)
(def driver-event-specs core/driver-event-specs)
(def start-source! core/start-source!)

;; Registry functions
(def drivers* core/drivers*)
(def normalize-driver-id core/normalize-driver-id)
(def event-type core/event-type)
(def driver-event-types core/driver-event-types)
(def make-static-driver core/make-static-driver)
(def register-driver! core/register-driver!)
(def register-drivers! core/register-drivers!)
(def unregister-driver! core/unregister-driver!)
(def clear-drivers! core/clear-drivers!)
(def driver core/driver)
(def registered-driver? core/registered-driver?)
(def registered-driver-ids core/registered-driver-ids)
(def emitted-event-types core/emitted-event-types)
(def source-listens core/source-listens)
(def listened-by-driver? core/listened-by-driver?)
(def source-event core/source-event)
