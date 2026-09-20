(ns knoxx.frontend.pages.cms.logic-test
  "Recovery regressions for source revision, review authority and unfinished human work."
  (:require [cljs.test :as t]
            [knoxx.frontend.pages.cms.controller :as controller]
            [knoxx.frontend.pages.cms.logic :as logic]))

(def snapshot
  "An actual source projection shape with a revision and immutable discussion head."
  {:document "wiki/example" :title "Shared knowledge" :content "Original source"
   :revision "sha256-source-1" :source_locale "en" :head nil :status "draft"
   :accepted false :stale false :history [] :lessons []})

(t/deftest source-projection-boundary
  (t/is (= snapshot (logic/assert-snapshot! snapshot)))
  (t/is (thrown? js/Error (logic/assert-snapshot! (dissoc snapshot :revision))))
  (t/is (thrown? js/Error (logic/assert-snapshot! (assoc snapshot :status "published")))))

(t/deftest clean-live-updates-follow-the-actual-source
  (let [next-source (assoc snapshot :content "Agent revision" :revision "sha256-source-2")
        editor (logic/receive-snapshot (logic/fresh-editor snapshot) next-source)]
    (t/is (= "Agent revision" (:content editor)))
    (t/is (= "sha256-source-2" (:base-revision editor)))
    (t/is (not (logic/dirty? editor)))
    (t/is (not (:conflict? editor)))))

(t/deftest dirty-source-survives-concurrent-agent-revision
  (let [draft (assoc (logic/fresh-editor snapshot) :content "Human unfinished draft")
        next-source (assoc snapshot :content "Agent revision" :revision "sha256-source-2")
        editor (logic/receive-snapshot draft next-source)]
    (t/is (= "Human unfinished draft" (:content editor)))
    (t/is (= "sha256-source-1" (:base-revision editor)))
    (t/is (= next-source (:snapshot editor)))
    (t/is (:conflict? editor))
    (t/is (not (logic/writable-draft? editor)))))

(t/deftest changed-review-head-does-not-block-a-current-source-save
  (let [draft (assoc (logic/fresh-editor snapshot) :content "Changed source")
        editor (logic/receive-snapshot draft (assoc snapshot :head "review/comment"))]
    (t/is (= "Changed source" (:content editor)))
    (t/is (logic/writable-draft? editor))))

(t/deftest review-notes-cannot-silently-rebind
  (let [form (assoc (logic/empty-review snapshot) :notes "Check terminology")]
    (t/is (logic/review-form-current? form snapshot))
    (t/is (not (logic/review-form-current? form (assoc snapshot :head "review/agent"))))
    (t/is (not (logic/review-form-current? form (assoc snapshot :revision "sha256-source-2"))))
    (t/is (logic/unsaved? {:review-form form}))
    (t/is (not (logic/unsaved? {:review-form (logic/empty-review snapshot)})))))

(t/deftest creation-and-empty-source-guards
  (let [initial {:title "" :content "" :garden "garden/local"}
        form (assoc initial :initial initial)]
    (t/is (not (logic/creation-dirty? form)))
    (t/is (logic/unsaved? {:creation (assoc form :title "Unfinished") }))
    (t/is (not (logic/writable-draft? (assoc (logic/fresh-editor snapshot) :content "  "))))))

(t/deftest explicit-command-denials-remain-distinct
  (t/is (logic/command-available? ["wiki_create" "wiki_review"] "wiki_create"))
  (t/is (not (logic/command-available? ["wiki_create" "wiki_review"] "wiki_save")))
  (t/is (not (logic/command-available? [] "wiki_publish")))
  (t/is (logic/command-available? nil "wiki_save")))

(t/deftest source-review-transitions-remain-separate
  (t/is (logic/review-action-available? snapshot "submit"))
  (t/is (not (logic/review-action-available? snapshot "accept")))
  (t/is (logic/review-action-available? (assoc snapshot :status "in_review") "accept"))
  (t/is (not (logic/review-action-available? (assoc snapshot :status "accepted") "submit")))
  (t/is (logic/review-action-available? (assoc snapshot :status "accepted") "request_changes")))

(t/deftest full-resource-identities-survive-inventory-normalization
  (let [row {:document {:document/id "wiki/name" :document/title "A wiki page"}}]
    (t/is (= "wiki/name" (logic/document-id row)))
    (t/is (= "A wiki page" (logic/document-title row)))
    (t/is (= {:documents [row]} (logic/normalize-inventory [row])))))

(t/deftest operation-identities-survive-only-identical-repair-retries
  (let [pending #js {:current nil} payload {:content "Draft" :expected_revision "r1"}
        first-id (controller/operation-id! pending [:save "wiki/name"] payload)]
    (t/is (= first-id (controller/operation-id! pending [:save "wiki/name"] payload)))
    (t/is (not= first-id (controller/operation-id! pending [:save "wiki/name"] (assoc payload :content "New intent"))))
    (t/is (not= first-id (controller/operation-id! pending [:save "wiki/other"] payload)))))

(t/deftest published-intent-is-not-observed-publication
  (let [target {:id "publication/es" :document "wiki/name" :garden "garden/local" :locale "es"
                :revision "r1" :path "/es/name" :desired "published" :observed nil
                :blockers ["source-review-required"]}
        result {:document "wiki/name" :publications [target]}]
    (t/is (= result (logic/assert-publications! result)))
    (t/is (nil? (get-in result [:publications 0 :observed])))
    (t/is (thrown? js/Error (logic/assert-publications! (assoc result :publications [(dissoc target :observed)]))))))
