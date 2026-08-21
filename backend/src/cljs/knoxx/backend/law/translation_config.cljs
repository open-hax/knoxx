(ns knoxx.backend.law.translation-config
  "Contracts for Knoxx-owned translation pipeline configuration.

  Model references are **strings**, matching `:model/id` in the model resource
  catalog. This deviates from the card's pseudocode, which sketched
  `:translation/model :models/glm-5` as a keyword, because real catalog ids are
  not keyword-safe: they include `/`, `:` and `.` (`\"xiaomi/mimo-v2-pro\"`,
  `\"gemma4:31b\"`, `\"gpt-5.5\"`). Round-tripping those through `keyword` would
  mangle identity, and the catalog is the authority on how a model is named.
  Source locale and review policy stay keywords and are encoded explicitly.

  Every wire map is `{:closed true}`. Malli leaves `[:map ...]` open by default,
  so a body carrying the qualified `:translation/model` would otherwise validate
  happily *alongside* the unqualified `:model` the wire actually declares — and
  a PATCH would appear to succeed while leaving the authoritative model
  untouched."
  (:require [knoxx.backend.law.publication :as publication]))

;; ── Domain ─────────────────────────────────────────────────────────────────

(def ModelRef
  "A model id as the catalog spells it."
  publication/NonBlankString)

(def TranslationPipelineConfig
  [:map {:closed true}
   [:translation/model ModelRef]
   [:translation/source-locale publication/Locale]
   [:translation/default-review [:enum :required :none]]])

(def TranslationPipelineConfigPatch
  [:map {:closed true}
   [:translation/model ModelRef]])

;; ── Wire ───────────────────────────────────────────────────────────────────

(def TranslationConfigWireJson
  [:map {:closed true}
   [:model :string]
   [:source-locale :string]
   [:default-review [:enum "required" "none"]]])

(def TranslationConfigPatchJson
  [:map {:closed true}
   [:model :string]])
