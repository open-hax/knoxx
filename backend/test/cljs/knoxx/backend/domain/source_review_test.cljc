(ns knoxx.backend.domain.source-review-test
  (:require #?(:clj [clojure.test :as test]
               :cljs [cljs.test :as test])
            [knoxx.backend.domain.source-review :as review]
            [knoxx.backend.shape.source-review :as shape]))

(def ^:private scope {:org-id "org-a" :project "wiki" :document :docs/introduction})
(def ^:private actor {:id "member-a" :kind :human})
(def ^:private snapshot {:document :docs/introduction :revision "sha256-original"
               :source-locale :en :title "Introduction" :content "Original source."})

(defn- command
  [id action previous]
  {:operation-id id :revision (:revision snapshot) :source-locale :en
   :expected-head previous :action action})

(defn- event
  [id action previous & [extra]]
  (review/event-for-command scope actor (merge (command id action previous) extra)
                            "2026-09-12T06:00:00Z"))

(defn- error-code
  [f]
  (try (f) nil
       (catch #?(:clj Exception :cljs :default) err (:code (ex-data err)))))

(def ^:private submitted [(event "submit-1" :submit nil)])
(def ^:private accepted (conj submitted (event "accept-1" :accept "submit-1")))

(test/deftest source-review-lifecycle
  (test/is (= :draft (:status (review/project [] scope snapshot))))
  (test/is (false? (:stale (review/project [] scope snapshot))))
  (test/is (= :in-review (:status (review/project submitted scope snapshot))))
  (test/is (true? (review/accepted? accepted "sha256-original" :en)))
  (let [rejected (conj accepted
                       (event "reject-1" :request-changes "accept-1"
                              {:notes "The conclusion needs a source."}))]
    (test/is (= :needs-revision (:status (review/project rejected scope snapshot))))
    (test/is (false? (review/accepted? rejected "sha256-original" :en)))
    (test/is (= 3 (count (:history (review/project rejected scope snapshot)))))))

(test/deftest acceptance-requires-prior-submission
  (test/is (= "source_review_transition_refused"
         (error-code #(review/decide [] scope actor snapshot
                                    (command "accept" :accept nil) "now")))))

(test/deftest changed-bytes-or-declared-language-invalidate-acceptance
  (doseq [changed [(assoc snapshot :revision "sha256-next" :content "Changed.")
                   (assoc snapshot :source-locale :fr)]]
    (let [projection (review/project accepted scope changed)]
      (test/is (false? (:accepted projection)))
      (test/is (= :draft (:status projection)))
      (test/is (true? (:stale projection)))
      (test/is (= accepted (:history projection))))))

(test/deftest stale-command-never-accepts-new-content
  (doseq [changed [(assoc snapshot :revision "sha256-next")
                   (assoc snapshot :source-locale :fr)]]
    (test/is (= "source_review_stale_revision"
           (error-code #(review/decide submitted scope actor changed
                                      (command "accept-new" :accept "submit-1") "now"))))))

(test/deftest exact-retry-retains-first-receipt-after-source-edit
  (let [result (review/decide accepted scope actor
                              (assoc snapshot :revision "sha256-next")
                              (command "accept-1" :accept "submit-1") "later")]
    (test/is (true? (:existing? result)))
    (test/is (= (peek accepted) (:event result)))))

(test/deftest changed-reuse-of-operation-identity-conflicts
  (doseq [[attempt-actor changed-command]
          [[actor (assoc (command "accept-1" :accept "submit-1") :notes "changed")]
           [actor (command "accept-1" :accept "accept-1")]
           [actor (command "accept-1" :accept nil)]
           [(assoc actor :id "another-person") (command "accept-1" :accept "submit-1")]]]
    (test/is (= "source_review_operation_conflict"
           (error-code #(review/decide accepted scope attempt-actor snapshot
                                      changed-command "later"))))))

(test/deftest stale-head-refuses-overwriting-another-reviewer
  (test/is (= "source_review_stale_head"
         (error-code #(review/decide accepted scope actor snapshot
                                    (assoc (command "note" :comment "submit-1") :notes "later")
                                    "now")))))

(test/deftest invalid-history-fails-closed
  (test/testing "wrong order, duplicate operations and cross-tenant facts"
    (doseq [history [(vec (reverse accepted))
                     (conj accepted (peek accepted))
                     [(assoc (first submitted) :review/scope (assoc scope :org-id "org-b"))]]]
      (test/is (some? (error-code #(review/project history scope snapshot)))))))

(test/deftest requesting-changes-needs-actionable-feedback
  (test/is (= "source_review_feedback_required"
         (error-code #(review/decide submitted scope actor snapshot
                                    (command "changes" :request-changes "submit-1") "now")))))

(test/deftest corrections-are-immutable-proposals-not-accepted-source-bytes
  (let [correction {:before "Original" :after "Revised" :reason "Clarify wording."}
        proposal (event "changes" :request-changes "submit-1" {:corrections [correction]})
        projected (review/project (conj submitted proposal) scope snapshot)]
    (test/is (= "Original source." (:content projected)))
    (test/is (= [correction] (:review/corrections (peek (:history projected))))))
  (test/is (= "source_review_unapplied_correction"
         (error-code #(review/decide submitted scope actor snapshot
                                    (assoc (command "accept" :accept "submit-1")
                                           :corrections [{:before "Original" :after "Revised"
                                                          :reason "Clarify."}]) "now")))))

(test/deftest accepted-writing-lessons-survive-new-revisions-with-attribution
  (let [brainstorm (event "brainstorm" :comment nil
                         {:notes "Recall the last review." :lessons ["Name each source."]})
        history [brainstorm (event "submit" :submit "brainstorm")
                 (event "accept" :accept "submit")]
        lesson (first (review/learned-lessons history))]
    (test/is (empty? (review/learned-lessons [brainstorm])))
    (test/is (= "Name each source." (:text lesson)))
    (test/is (= actor (:actor lesson)))
    (test/is (= "brainstorm" (:review lesson)))
    (test/is (= [lesson] (:lessons (review/project history scope
                                             (assoc snapshot :revision "sha256-next")))))
    (test/is (empty? (review/learned-lessons
                 (conj history (event "reject" :request-changes "accept" {:notes "Unverified."})))))))

(test/deftest closed-wire-rejects-forged-authority-and-unknown-fields
  (let [wire {:operation_id "submit" :revision "sha256-original"
              :source_locale "en" :expected_head nil :action "submit"}]
    (test/is (= (command "submit" :submit nil) (shape/decode-command wire)))
    (doseq [field [:actor :org_id :project :document :unknown]]
      (test/is (= "source_review_invalid"
             (error-code #(shape/decode-command (assoc wire field "forged"))))))
    (test/is (= "source_review_invalid"
           (error-code #(shape/decode-command (dissoc wire :expected_head)))))))

(test/deftest comments-after-acceptance-cannot-inject-positive-memory
  (let [unreviewed (event "late-comment" :comment "accept-1"
                          {:lessons ["Unreviewed claim."]})
        history (conj accepted unreviewed)]
    (test/is (true? (review/accepted? history "sha256-original" :en)))
    (test/is (empty? (review/learned-lessons history)))))

(test/deftest wire-preserves-resource-namespace-and-history
  (let [wire (shape/projection->wire (review/project accepted scope snapshot))]
    (test/is (= "docs/introduction" (:document wire)))
    (test/is (= "accepted" (:status wire)))
    (test/is (= "en" (:source_locale wire)))
    (test/is (= "human" (get-in wire [:history 0 :actor :kind])))
    (test/is (not (contains? (first (:history wire)) :scope)))))
