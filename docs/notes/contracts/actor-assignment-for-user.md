---
title: "Actor Assignment for User — No More Hardcoded Admin"
category: contracts
created: 2026-04-22
original: 2026.04.22.10.29.07.md
status: note
---

I need an actor assigned to me, so we don't have to have that
hard coded system admin env var.

I should have an actor and a user assigned to me, with the
correct role.

the github oauth system should get my email from github, and
we use the email as the connonical user name

;; ─── policy contract ─────────────────────────────────────────
 :policy-check [:map {:closed false}
                [:id        :id]
                [:severity  [:enum :block :warn :note]]
                [:message   :string]
                [:check     {:expr :maybe ;; an expression that evaluates to a boolean
                             :rule :named-rule}]]

 :policy-contract [:map {:closed false}
                   [:contract/id       :id]
                   [:contract/kind     [:= :policy]]
                   [:contract/doc      {:optional true} :string]
                   [:contract/scope    {:optional true} :string]
                   [:contract/uses     {:optional true} [:vector :id]]
                   [:policy/invariants {:optional true} [:vector :policy-check]]
                   [:policy/required   {:optional true} [:vector :policy-check]]
                   [:policy/checked-by {:optional true} :keyword]]

