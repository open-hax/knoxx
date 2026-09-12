(ns knoxx.backend.extern.accepted-source-fixture
  "Exact source-review read fixtures for dispatch orchestration unit tests."
  (:require [knoxx.backend.infra.source-review :as review]
            [knoxx.backend.infra.wiki-runtime :as runtime]
            [knoxx.backend.law.source-review :as law]))

(defn ^:async with-source!
  "Supply one already accepted snapshot while keeping acceptance-facts! real.
   These tests stub resource files and revisions; they do not prove review
   admission or filesystem provenance. Any different tenant/document read fails."
  [scope snapshot body]
  (let [dependencies {:fixture :accepted-source}]
    (with-redefs [runtime/source-dependencies (fn ([] dependencies) ([_] dependencies))
                  review/read! (fn [_ requested actual-dependencies]
                                 (law/assert-valid! :source-review/scope law/Scope requested)
                                 (when-not (and (= scope (select-keys requested (keys scope)))
                                                (= dependencies actual-dependencies))
                                   (throw (ex-info "Unseeded source review read"
                                                   {:status 404 :scope requested})))
                                 snapshot)]
      (await (body)))))
