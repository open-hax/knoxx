(ns knoxx.backend.infra.wiki-admission-scope-test
  "The real Wiki command must retain acting membership at translation admission."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.source-review :as domain]
            [knoxx.backend.infra.publication-admission-hook :as admission]
            [knoxx.backend.infra.source-review :as review]
            [knoxx.backend.infra.translation-dispatch :as dispatch]
            [knoxx.backend.infra.wiki-commands :as commands]
            [knoxx.backend.infra.wiki-runtime :as runtime]))

(def ^:private scope {:org-id "org-a" :project "wiki" :document :docs/page})
(def ^:private actor {:id "actor-a" :kind :human})
(def ^:private source {:document :docs/page :revision "revision-1" :source-locale :en
                      :title "Page" :content "Human-reviewed content."})
(def ^:private context {:org-id "org-a" :user-id "actor-a"
                       :permissions ["org.publications.review"]})
(def ^:private wire {:operation_id "accept-1" :revision "revision-1" :source_locale "en"
                    :expected_head "submit-1" :action "accept"})
(def ^:private intent {:publication/garden :gardens/wiki :publication/document :docs/page
                      :document/source-locale :en})

(defn- accepted-result []
  (let [command {:revision "revision-1" :source-locale :en}
        history [(domain/event-for-command scope actor
                   (assoc command :operation-id "submit-1" :action :submit :expected-head nil) "first")
                 (domain/event-for-command scope actor
                   (assoc command :operation-id "accept-1" :action :accept :expected-head "submit-1") "second")]]
    {:existing? false :event (peek history) :review (domain/project history scope source)}))

(defn- ^:async attempt! [ctx mutations dispatches]
  (with-redefs [runtime/source-dependencies (fn ([] {}) ([_runtime] {}))
                review/command! (fn [_ review-scope _ _ _]
                                  (swap! mutations conj review-scope) (accepted-result))
                admission/admit! (fn [admission-scope selection]
                                   (swap! dispatches conj [admission-scope selection])
                                   (dispatch/dispatch-context intent admission-scope "source-digest"))]
    (try {:result (await (commands/review! {:session-project-name "wiki"} ctx "docs/page" wire))}
         (catch :default error {:error (assoc (or (ex-data error) {}) :message (str error))}))))

(deftest ^:async accepted-source-retains-verified-membership-without-changing-review-identity
  (doseq [membership [{:membership-id "member-a"} {:membershipId "member-a"}
                      {:membership {:id "member-a"}}]]
    (let [mutations (atom []) dispatches (atom [])
          result (await (attempt! (merge context membership) mutations dispatches))]
      (is (nil? (:error result)))
      (is (true? (get-in result [:result :review :accepted])))
      (is (= [scope] @mutations))
      (is (= [[{:org-id "org-a" :project "wiki" :membership-id "member-a"}
               {:document :docs/page}]] @dispatches)))))

(deftest ^:async acceptance-without-membership-refuses-before-durable-review
  (doseq [membership [nil "" "   "]]
    (let [mutations (atom []) dispatches (atom [])
          result (await (attempt! (assoc context :membership-id membership) mutations dispatches))]
      (is (= 403 (get-in result [:error :status])))
      (is (= "wiki_membership_required" (get-in result [:error :code])))
      (is (empty? @mutations))
      (is (empty? @dispatches)))))
