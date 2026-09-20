# Membership actor coordinates

Once a Mongo membership has an actor ID, ordinary user, role and contract
projection operations may retain that normalized ID but cannot replace it.
An unassigned membership can receive its first actor. Concurrent initial
assignments use compare-and-set; distinct contenders cannot both succeed.

A conflicting assigned actor returns HTTP status 409 with code
`membership_actor_immutable` before the rejected operation writes roles,
tool policies, profile fields, actor contracts or audit entries. Full contract
projection preflights its input batch, so a historical conflicting declaration
cannot overwrite the current actor or roles before being refused. Existing
contract files are retained; email matches never authorize their deletion.

This is a deliberate migration limit. Actor IDs participate in immutable
identity bindings, so ordinary policy updates cannot safely perform actor
reassignment. An explicit migration that reconciles bindings and contract
history is outside this change. Initial creation across separate resources
still does not claim a database/filesystem transaction or rollback.

The focused `mongo-policy-mutations-test` and `mongo-policy-directory-test`
namespaces cover admission and same/initial assignment. The opt-in
`mongo-actor-coordinate-e2e` namespace runs against an owned Mongo process and
temporary contract files; it covers concurrent initial assignments, rejected
native mutations, and complete projection of conflicting historical contracts.
