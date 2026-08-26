(ns knoxx.backend.extern.fastify.translation-review-test
  "The approval boundary: what a caller may and may not put in the request.

  Every property here is one an untrusted caller would otherwise control. The
  principal, the timestamp and the tenant are all attributed by the server, and
  the only way to be sure of that is to try to send them."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.fastify.translation-review :as adapter]
            [knoxx.backend.law.translation-evidence :as law]))

(defn- request
  [body]
  (js-obj "body" (clj->js body)))

(def ^:private valid-body
  {:document "knoxx.docs/probe"
   :garden "knoxx.docs/promethean"
   :locale "es"
   :revision "sha256-aaa111bbb222"
   :translation_revision "sha256-aaa111bbb222+es@batch-1"})

(deftest a-well-formed-body-decodes-to-the-identities-receipts-are-keyed-by
  (let [decoded (adapter/decode-request (request valid-body))]
    (testing "the document, garden and locale become keywords"
      (is (= :knoxx.docs/probe (:review/document decoded)))
      (is (= :knoxx.docs/promethean (:review/garden decoded)))
      (is (= :es (:review/locale decoded))))

    (testing "revisions stay opaque strings"
      ;; A revision is opaque text. Decoding it to a keyword would invent
      ;; structure that is not there.
      (is (= "sha256-aaa111bbb222" (:review/revision decoded)))
      (is (= "sha256-aaa111bbb222+es@batch-1" (:review/translation-revision decoded))))))

(deftest a-caller-cannot-supply-its-own-principal-or-timestamp
  ;; The request contract is closed, so the fields attribution owns cannot be
  ;; smuggled through the body.
  (doseq [field [:principal :review/principal :at :review/at :org_id :project]]
    (is (thrown? js/Error
                 (adapter/decode-request (request (assoc valid-body field "forged"))))
        (str field " was accepted"))))

(deftest every-field-is-required-and-must-not-be-blank
  (doseq [field [:document :garden :locale :revision :translation_revision]]
    (testing (str field " is required")
      (is (thrown? js/Error (adapter/decode-request (request (dissoc valid-body field))))))

    (testing (str field " may not be blank")
      (is (thrown? js/Error (adapter/decode-request (request (assoc valid-body field "")))))
      (is (thrown? js/Error (adapter/decode-request (request (assoc valid-body field "  "))))))))

(deftest an-unqualified-document-or-garden-is-refused
  (testing "a bare name is a different document from the qualified one"
    (is (thrown? js/Error
                 (adapter/decode-request (request (assoc valid-body :document "probe"))))))

  (testing "the same holds for the garden the approval is scoped to"
    ;; An unqualified garden is a different garden, and an approval filed
    ;; against one the receipts are not keyed by can never match — so it would
    ;; read as 'nothing to approve' rather than as a malformed request.
    (is (thrown? js/Error
                 (adapter/decode-request (request (assoc valid-body :garden "promethean")))))))

(deftest a-selector-revision-is-refused-in-either-position
  ;; This is the boundary where a revision arrives as decoded wire input, so the
  ;; string spelling of the selector is the one that has to be caught.
  (is (thrown? js/Error
               (adapter/decode-request (request (assoc valid-body
                                                       :revision "source/current")))))
  (is (thrown? js/Error
               (adapter/decode-request (request (assoc valid-body
                                                       :translation_revision "source/current"))))))

(deftest the-principal-comes-from-the-auth-context
  (testing "any one durable identity is enough"
    (is (= {:principal/user-email "a@b.c"}
           (adapter/principal-of {:user-email "a@b.c"})))
    (is (= {:principal/user-id "u1"} (adapter/principal-of {:user-id "u1"})))
    (is (= {:principal/membership-id "m1"} (adapter/principal-of {:membership-id "m1"}))))

  (testing "a context identifying nobody cannot produce review evidence"
    ;; Evidence attributable to nobody is indistinguishable from evidence nobody
    ;; produced.
    (is (thrown? js/Error (adapter/principal-of {})))
    (is (thrown? js/Error (adapter/principal-of nil)))))

(deftest the-review-scope-is-taken-from-context-and-config
  (testing "the organization comes from the context"
    (is (= "org-1" (:org-id (adapter/review-scope {} {:org-id "org-1"
                                                      :user-email "a@b.c"})))))

  (testing "the project comes from configuration, matching where batches are filed"
    (is (= "knoxx-session"
           (:project (adapter/review-scope {:session-project-name "knoxx-session"}
                                           {:org-id "org-1" :user-email "a@b.c"})))))

  (testing "an approval cannot be filed under an invented tenant"
    (is (thrown? js/Error (adapter/review-scope {} {:user-email "a@b.c"})))))

(deftest responses-distinguish-recorded-existing-and-refused
  (testing "a first approval is 201"
    (is (= 201 (:status (adapter/response-for {:approval/status :recorded
                                               :approval {:review/state :approved}})))))

  (testing "an already-recorded approval is 200, not a conflict"
    ;; An honest double-click is not something a reviewer has to resolve.
    (is (= 200 (:status (adapter/response-for {:approval/status :existing
                                               :approval {:review/state :approved}})))))

  (testing "a refusal is 409 and carries its typed evidence to the caller"
    (let [{:keys [status body]}
          (adapter/response-for {:approval/refusal
                                 {:refusal/type :translation-revision-mismatch
                                  :refusal/requested "a"
                                  :refusal/recorded "b"}})]
      (is (= 409 status))
      (is (true? (:refused body)))
      (is (= :translation-revision-mismatch (:refusal/type (:refusal body))))))

  (testing "every refusal type the law can produce has a status chosen for it"
    ;; Compared against `law/approval-refusal-types`, not against a second copy
    ;; of the adapter's own keys. A restated list validates the table against
    ;; itself: adding a refusal type to the law and forgetting the adapter left
    ;; both this assertion and `refusal-status` unchanged and still agreeing,
    ;; while the new type fell through `get`'s default to a status nobody chose.
    (is (= law/approval-refusal-types (set (keys adapter/refusal-status))))))
