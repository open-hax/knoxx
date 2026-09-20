(ns knoxx.backend.law.event-scope-test
  "Portable closed-scope validation and replacement of ambient configuration."
  (:require [clojure.test :as t]
            [knoxx.backend.law.event-scope :as scope]))

(def admitted
  "Coordinates verified before the event was emitted."
  {:org-id "org-a" :membership-id "member-a" :project "project-a"})

(t/deftest scope-refuses-incomplete-or-forged-authority
  (t/is (= admitted (scope/assert-scope! admitted)))
  (doseq [value [nil {} (dissoc admitted :org-id) (dissoc admitted :membership-id)
                 (assoc admitted :org-id " ") (assoc admitted :membership-id 42)
                 (assoc admitted :project nil) (assoc admitted :project " ")
                 (assoc admitted :roleSlugs ["system_admin"])
                 (assoc admitted :permissions ["*"])
                 (assoc admitted :event/trusted? true)]]
    (t/is (= "invalid_event_scope"
             (try (scope/assert-scope! value) nil
                  (catch #?(:clj Exception :cljs :default) error
                    (:code (ex-data error))))))))

(t/deftest missing-project-removes-ambient-project
  (let [ambient {:openplanner-org-id "ambient-org" :session-project-name "ambient-project"
                 :unrelated true}]
    (t/is (= {:openplanner-org-id "org-a" :session-project-name "project-a" :unrelated true}
             (scope/scoped-config ambient admitted)))
    (t/is (= {:openplanner-org-id "org-a" :unrelated true}
             (scope/scoped-config ambient (dissoc admitted :project))))))
