(ns knoxx.backend.extern.openplanner-translation-mongo
  "Compatibility facade for Knoxx-owned ClojureScript translation storage."
  (:require [knoxx.backend.extern.openplanner-translation-mongo.batches :as batches]
            [knoxx.backend.extern.openplanner-translation-mongo.common :as common]
            [knoxx.backend.extern.openplanner-translation-mongo.documents :as documents]
            [knoxx.backend.extern.openplanner-translation-mongo.segments :as segments]))

(defn ensure-indexes!
  "Ensure translation indexes and migrations; returns a promise resolving true."
  []
  (common/ensure-indexes!))

(defn list-segments!
  "List tenant-scoped translation segments."
  [opts]
  (segments/list-segments! opts))

(defn segment!
  "Load one tenant-scoped translation segment."
  [segment-id opts]
  (segments/segment! segment-id opts))

(defn create-segment!
  "Create or update one tenant-scoped translation segment."
  [input]
  (segments/create-segment! input))

(defn create-segments-batch!
  "Import a batch of tenant-scoped translation segments."
  [payload]
  (segments/create-segments-batch! payload))

(defn label-segment!
  "Label one tenant-scoped translation segment."
  [segment-id payload]
  (segments/label-segment! segment-id payload))

(defn export-sft!
  "Export tenant-scoped approved segments as SFT rows."
  [opts]
  (segments/export-sft! opts))

(defn manifest!
  "Build a tenant-scoped translation manifest."
  [opts]
  (documents/manifest! opts))

(defn documents!
  "List tenant-scoped translated documents."
  [opts]
  (documents/documents! opts))

(defn document!
  "Load one tenant-scoped translated document."
  [document-id target-lang opts]
  (documents/document! document-id target-lang opts))

(defn review-document!
  "Apply a tenant-scoped document review."
  [document-id target-lang payload]
  (documents/review-document! document-id target-lang payload))

(defn create-batch!
  "Create a tenant- and membership-scoped translation batch."
  [payload]
  (batches/create-batch! payload))

(defn list-batches!
  "List tenant-scoped translation batches."
  [opts]
  (batches/list-batches! opts))

(defn next-batch!
  "Claim the next tenant-scoped translation batch."
  [opts]
  (batches/next-batch! opts))

(defn batch!
  "Load one tenant-scoped translation batch."
  [batch-id opts]
  (batches/batch! batch-id opts))

(defn update-batch!
  "Update one tenant-scoped translation batch."
  [batch-id payload]
  (batches/update-batch! batch-id payload))