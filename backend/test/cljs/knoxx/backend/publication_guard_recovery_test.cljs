(ns knoxx.backend.publication-guard-recovery-test
  "Legacy mutation routes share Wiki owner, capability and source revision gates."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.document-admission :as admission]
            [knoxx.backend.extern.fastify.cms-publication :as cms-routes]
            [knoxx.backend.extern.publication-guard-fixture :as fixture]
            [knoxx.backend.extern.source-authoring :as files]
            [knoxx.backend.infra.publication-reconciler :as reconciler]
            [knoxx.backend.infra.publication-runtime :as production]
            [knoxx.backend.infra.routes.cms-publication :as facade]
            [knoxx.backend.infra.routes.publication-reconcile :as reconcile-routes]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.source-review :as review]
            [knoxx.backend.infra.wiki-commands :as commands]
            [knoxx.backend.infra.wiki-publication :as wiki]
            [knoxx.backend.infra.wiki-runtime :as runtime]))

(def ^:private owner {:org-id "owner" :permissions ["org.publications.publish"]})
(def ^:private document {:document/id :docs/probe :document/org-id "owner" :document/visibility :public
                        :document/title "Probe" :document/source-locale :en :document/source {:path "probe.md"}})
(def ^:private intent {:publication/id :docs/probe-en :publication/document :docs/probe
                      :publication/garden :docs/garden :publication/locale :en
                      :publication/revision :source/current :publication/state :draft
                      :publication/path "/probe" :translation/review :none})
(def ^:private index {:documents {:docs/probe document} :publications [intent] :gardens {}})

(defn- ^:async status [operation]
  (try (await (operation)) nil (catch :default error (:status (ex-data error)))))

(defn- open! [context]
  (fixture/open! context
                (fn [app handlers]
                  (cms-routes/register-cms-publication-routes! app nil {} handlers)
                  (reconcile-routes/register-publication-reconcile-routes! app nil {} handlers))))

(defn- endpoints [revision]
  [["PATCH" "/api/cms/publications/intents/docs%2Fprobe-en" {:state "published" :expected_revision revision}]
   ["POST" "/api/publications/reconcile" {:publicationId "docs/probe-en" :expected_revision revision}]])

(defn- ^:async with-source! [revision reads effects operation]
  (with-redefs [publications/publication-index! (fn [_] index)
                runtime/source-dependencies (fn ([] {}) ([_runtime] {}))
                review/read! (fn [_ scope _] (swap! reads conj scope) {:revision @revision})
                production/configured? (constantly true)
                production/make-runtime! (fn [& _] {:reconciler :fixture})
                facade/set-publication-state! (fn [& args] (swap! effects conj [:state args]) {:desired "published"})
                reconciler/reconcile! (fn [& args] (swap! effects conj [:reconcile args]) {:receipt/type :publication/noop})]
    (await (operation))))

(deftest ^:async both-legacy-routes-require-publish-capability-and-respect-explicit-tool-denial
  (let [context (atom owner) app (open! context) reads (atom []) effects (atom [])]
    (try
      (await (with-source! (atom "revision-1") reads effects
               (^:async fn []
                 (doseq [denied [nil {:org-id "owner" :permissions ["org.publications.manage"]}
                                  (assoc owner :tool-policies [{:tool-id "wiki_publish" :effect "deny"}])
                                  {:org-id "owner" :role-slugs ["system-admin"]
                                   :tool-policies [{:tool-id "wiki_publish" :effect "deny"}]}]]
                   (reset! context denied)
                   (doseq [[method path body] (endpoints "revision-1")]
                     (is (= 403 (:status (await (fixture/request! app method path body)))))))
                 (is (empty? @reads)) (is (empty? @effects)))))
      (finally (await (fixture/close! app))))))

(deftest ^:async public-cross-tenant-visibility-never-confers-mutation-ownership
  (let [context (atom (assoc owner :org-id "foreign")) app (open! context)
        reads (atom []) effects (atom [])]
    (try
      (is (admission/document-visible-to-org? {:org-id "foreign"} document))
      (await (with-source! (atom "revision-1") reads effects
               (^:async fn []
                 (doseq [[method path body] (endpoints "revision-1")]
                   (is (= 404 (:status (await (fixture/request! app method path body))))))
                 (is (empty? @reads)) (is (empty? @effects)))))
      (finally (await (fixture/close! app))))))

(deftest ^:async both-legacy-routes-bind-mutation-to-the-current-source-revision
  (let [context (atom owner) app (open! context) reads (atom []) effects (atom [])]
    (try
      (await (with-source! (atom "revision-2") reads effects
               (^:async fn []
                 (doseq [[method path body] (endpoints "revision-1")]
                   (is (= 409 (:status (await (fixture/request! app method path body))))))
                 (is (empty? @effects))
                 (doseq [[method path body] (endpoints nil)]
                   (is (= 400 (:status (await (fixture/request! app method path (dissoc body :expected_revision)))))))
                 (is (empty? @effects))
                 (doseq [[method path body] (endpoints "revision-2")]
                   (is (= 200 (:status (await (fixture/request! app method path body))))))
                 (is (= [:state :reconcile] (mapv first @effects))))))
      (finally (await (fixture/close! app))))))

(deftest ^:async unknown-publication-is-an-opaque-not-found-before-source-lookup
  (let [reads (atom []) effects (atom [])]
    (await (with-source! (atom "revision-1") reads effects
             (^:async fn []
               (is (= 404 (await (status #(wiki/with-owned-revision! {} owner :docs/missing "revision-1"
                                           (fn [] (swap! effects conj :unexpected)))))))
               (is (empty? @reads)) (is (empty? @effects)))))))

(deftest ^:async source-change-while-awaiting-the-shared-lock-invalidates-the-publish-cas
  (let [revision (atom "revision-1") reads (atom []) effects (atom [])
        release (fixture/barrier) entered (fixture/barrier)
        scope (commands/scope {} owner :docs/probe)]
    (await (with-source! revision reads effects
             (^:async fn []
               (let [edit (files/with-document-lock! scope
                            (^:async fn [] ((:release! entered)) (await (:wait release))
                              (reset! revision "revision-2")))
                     _ (await (:wait entered))
                     publish (wiki/with-owned-revision! {} owner :docs/probe-en "revision-1"
                               (fn [] (swap! effects conj :publish)))
                     result (status (fn [] publish))]
                 (is (empty? @reads) "Revision is read after the pending source edit releases the lock")
                 ((:release! release))
                 (await edit)
                 (is (= 409 (await result)))
                 (is (= [scope] @reads))
                 (is (empty? @effects))))))))
