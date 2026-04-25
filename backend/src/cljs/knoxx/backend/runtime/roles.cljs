(ns knoxx.backend.runtime.roles
  (:require [knoxx.backend.contracts.roles :as impl]))

;; Backwards-compat shim: role/capability contract semantics now live under
;; knoxx.backend.contracts.*. Keep runtime.* as process/runtime-only.

(def role-aliases impl/role-aliases)
(def role-slug->file impl/role-slug->file)
(def cap-slug->file impl/cap-slug->file)
(def list-role-slugs impl/list-role-slugs)
(def normalize-role impl/normalize-role)
(def role-capability-ids impl/role-capability-ids)
(def capability-tool-ids impl/capability-tool-ids)
(def role-tool-ids impl/role-tool-ids)
(def role-tools impl/role-tools)
