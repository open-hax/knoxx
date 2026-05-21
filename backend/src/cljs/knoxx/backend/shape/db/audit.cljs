(ns knoxx.backend.shape.db.audit
  "Pure HoneySQL query builders for audit events. No execution."
  (:require [honey.sql.helpers :as h]))

(defn insert-event
  [{:keys [actor-user-id actor-membership-id org-id action
           resource-kind resource-id before-json after-json]}]
  (-> (h/insert-into :audit_events)
      (h/values [{:actor_user_id        (when actor-user-id [:cast actor-user-id :uuid])
                  :actor_membership_id  (when actor-membership-id [:cast actor-membership-id :uuid])
                  :org_id               (when org-id [:cast org-id :uuid])
                  :action               action
                  :resource_kind        resource-kind
                  :resource_id          resource-id
                  :before_json          (when before-json [:raw before-json])
                  :after_json           (when after-json [:raw after-json])}])))
