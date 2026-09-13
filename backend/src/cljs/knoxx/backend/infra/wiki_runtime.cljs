(ns knoxx.backend.infra.wiki-runtime
  "Composition of independently replaceable durable Wiki service providers."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.extern.clock :as clock]
            [knoxx.backend.infra.clio-source-authoring-store :as source]
            [knoxx.backend.infra.clio-source-review-store :as review]
            [knoxx.backend.infra.clio-translation-evidence-store :as evidence]
            [knoxx.backend.infra.clio-translation-split-store :as split]
            [knoxx.backend.infra.stores.translation-evidence-registry :as evidence-registry]
            [knoxx.backend.infra.stores.translation-split-registry :as split-registry]))

(defonce runtime* (atom nil))

(defn current
  "Return the installed Wiki composition, or nil before successful startup."
  [] @runtime*)

(defn require-runtime!
  "Refuse operations when durable Wiki services were not successfully opened."
  []
  (or (current)
      (throw (ex-info "Wiki persistence is unavailable"
                      {:status 503 :code "wiki_persistence_unavailable"}))))

(defn install!
  "Install a complete composition only after every provider has opened."
  [runtime]
  (when-not (every? #(some? (get runtime %))
                   [:source-store :source-review-store :evidence-store :split-store :now!])
    (throw (ex-info "Wiki composition is incomplete"
                    {:status 503 :code "wiki_composition_incomplete"})))
  (reset! evidence-registry/store* (:evidence-store runtime))
  (reset! split-registry/store* (:split-store runtime))
  (reset! runtime* runtime))

(defn open!
  "Open canonical Clio ledgers without requiring Mongo or a model service."
  ([config] (open! config clock/instant-iso))
  ([config now!]
   (let [directory (:wiki-directory config)]
     (when-not (and (string? directory) (not (str/blank? directory)) (fn? now!))
       (throw (ex-info "Wiki ledger directory and clock are required"
                       {:status 503 :code "wiki_configuration_invalid"})))
     (install!
      {:directory directory
       :source-store (source/open! {:directory (str directory "/source")})
       :source-review-store (review/open! {:directory (str directory "/source-review")})
       :evidence-store (evidence/open! {:directory (str directory "/translation-evidence")})
       :split-store (split/open! {:directory (str directory "/translation-splits")
                                 :digest-hex crypto/sha256-hex})
       :now! now!}))))

(defn source-dependencies
  "Provide the canonical source/review ports to shared command services."
  ([] (source-dependencies (require-runtime!)))
  ([runtime]
   {:provider (:source-review-store runtime)
    :source-provider (:source-store runtime)
    :now! (:now! runtime)}))
