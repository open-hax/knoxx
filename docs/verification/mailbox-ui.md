# Mailbox commands and live views

The existing `/mail` page now composes messages, reads canonical full content,
and acknowledges received messages. Its controls consume the current mailbox
capabilities, including explicit tool denials and mode-specific permissions.
Human composition sends the same finite target/content/mode command as the
agent tool. The server supplies tenant and actor authority.

Failed or uncertain sends preserve the draft and reuse the operation ID for an
identical retry. Editing the intent generates a new ID. Only a confirmed
delivered/acknowledged response clears the draft. The page displays the
provider's actual durability marker. It does not infer persistence from HTTP
success.

Mailbox SSE refreshes the inventory and an open full-message view; reconnect
also reconciles current state. Neither path clears the compose draft. Old
requests cannot overwrite a later filter or message selection, and leaving the
page closes its live subscription. Navigation guards protect unfinished text.

`scripts/mail-browser-tour.mjs` is a supervisor helper: give it a real page,
the supervisor's disposable principal/Clio storage, screenshot callback and a
real delegated `agentSend` adapter. It drives composition, reads past the
bounded preview, observes a live agent message without clicking Refresh,
preserves the human draft, acknowledges the agent message, and checks anonymous
denial. The enclosing supervisor must own fixture creation and cleanup and
record the exact compiled artifacts. This document does not claim a browser
pass before that fresh run completes.

The Clio mailbox is the implemented canonical provider. This frontend does not
add Mongo or OpenPlanner mailbox ports or claim those providers support the
same operations. Nonlocal delivery modes additionally require their selected
conversation/event services; mailbox-only delivery requires no live model turn.
