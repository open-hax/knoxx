(ns knoxx.backend.mcp-actor-ascription-test
  "Which actor an MCP tool call runs as, and how it is carried there.

   The bug these pin: Discord and Bluesky tools over MCP resolved no actor at
   all, so every credential read threw \"No current actor_id is available\". The
   actor was knowable the whole time — knoxx_memberships carries actor_id — but
   the OAuth code and token records never carried it, and
   domain.actor.credentials read only agent-context, which the MCP surface never
   sets. Verified in production on 2026-08-05 with a fully provisioned actor:
   tools listed, credentials resolvable by actor_id, every call still failed.

   Two halves are tested here: the law that says which actor may be named, and
   the scope that carries it without leaking across concurrent work."
  (:require [cljs.test :refer [deftest is testing async]]
            [knoxx.backend.domain.actor.acting :as acting]
            [knoxx.backend.domain.actor.credentials :as credentials]
            [knoxx.backend.domain.agent.agent-context :as agent-context]
            [knoxx.backend.law.mcp-oauth :as law]))

;; ─────────────────────────────────────────────────────────
;; law/actor-grantable? — which actor a membership may name
;; ─────────────────────────────────────────────────────────

(deftest actor-grantable-is-identity-today
  (testing "a membership may name its own actor"
    (is (law/actor-grantable? "open_hax" "open_hax")))
  (testing "and no other, because no grant says otherwise"
    (is (not (law/actor-grantable? "open_hax" "discord_automation")))))

(deftest actor-grantable-refuses-blanks
  (testing "a context that resolved no actor cannot authorize one"
    (is (not (law/actor-grantable? "" "open_hax")))
    (is (not (law/actor-grantable? nil "open_hax"))))
  (testing "and a blank request is not satisfied by a real membership actor"
    (is (not (law/actor-grantable? "open_hax" "")))
    (is (not (law/actor-grantable? "open_hax" nil))))
  (testing "two blanks are not a match either — otherwise a session with no
            actor would authorize the actor \"\", which owns nothing and would
            report as present"
    (is (not (law/actor-grantable? "" "")))
    (is (not (law/actor-grantable? nil nil)))))

(deftest actor-grantable-ignores-surrounding-space
  (testing "an id that round-tripped through a form field still matches"
    (is (law/actor-grantable? "open_hax" "  open_hax  "))
    (is (law/actor-grantable? " open_hax" "open_hax")))
  (testing "but whitespace is not an actor"
    (is (not (law/actor-grantable? "   " "   ")))))

;; ─────────────────────────────────────────────────────────
;; law/token-actor-honourable? — is a minted token's actor still valid
;;
;; A token is a bearer credential that outlives an edit to the membership it
;; came from. If an admin reassigns or clears the membership's actor_id, a token
;; that kept honouring its own copy would keep reading the old actor's
;; credentials until it expired.
;; ─────────────────────────────────────────────────────────

(deftest token-actor-honourable-when-unchanged
  (is (law/token-actor-honourable? "open_hax" "open_hax")))

(deftest token-actor-not-honourable-after-reassignment
  (testing "the membership now resolves to a different actor"
    (is (not (law/token-actor-honourable? "open_hax" "discord_automation"))))
  (testing "the membership's actor was cleared entirely"
    (is (not (law/token-actor-honourable? "open_hax" "")))
    (is (not (law/token-actor-honourable? "open_hax" nil)))))

(deftest token-without-an-actor-is-honourable
  (testing "a token minted before actors were carried keeps working; it simply
            has no actor, and a credential read fails saying exactly that"
    (is (law/token-actor-honourable? nil "open_hax"))
    (is (law/token-actor-honourable? "" "open_hax"))
    (is (law/token-actor-honourable? nil nil))))

;; ─────────────────────────────────────────────────────────
;; scope — carrying the actor to the credential read
;; ─────────────────────────────────────────────────────────

(deftest scope-is-empty-outside-a-run
  (is (nil? (acting/current-actor-id))))

(deftest scope-reads-inside-a-run
  (is (= "open_hax" (acting/run-as! "open_hax" #(acting/current-actor-id)))))

(deftest scope-does-not-outlive-a-run
  (acting/run-as! "open_hax" (fn [] nil))
  (is (nil? (acting/current-actor-id))
      "a scope must not leak into the next unit of work")
  (is (false? (acting/in-scope?))
      "and the scope itself must not outlive it either"))

(deftest scope-normalizes-its-actor-id
  (testing "an id arrives trimmed"
    (is (= "open_hax" (acting/run-as! "  open_hax\n" #(acting/current-actor-id)))))
  (testing "a blank reads as no actor, never as the actor \"\""
    (is (nil? (acting/run-as! "" #(acting/current-actor-id))))
    (is (nil? (acting/run-as! "   " #(acting/current-actor-id))))
    (is (nil? (acting/run-as! nil #(acting/current-actor-id))))))

;; ─────────────────────────────────────────────────────────
;; "No actor" is a positive fact, not an absence.
;;
;; A reader that sees no actor in scope must be able to tell "this work has none"
;; from "nobody said" — because only the second may consult the process-global
;; agent-context. Collapsing them let an actor-less MCP call borrow whatever
;; actor a concurrent agent turn was running as and spend its credentials, which
;; is the same leak the scope exists to prevent, reached through the fallback.
;; ─────────────────────────────────────────────────────────

(deftest a-blank-actor-still-enters-a-scope
  (testing "so a reader can tell definitive absence from nothing being said"
    (is (true? (acting/run-as! nil #(acting/in-scope?))))
    (is (true? (acting/run-as! "" #(acting/in-scope?))))
    (is (true? (acting/run-as! "open_hax" #(acting/in-scope?))))))

(deftest outside-every-scope-nothing-has-been-said
  (is (false? (acting/in-scope?)))
  (is (nil? (acting/current-actor-id))))

(deftest an-actor-less-scope-shadows-an-outer-actor
  (testing "the innermost claim wins even when it is 'no actor' — otherwise an
            actor-less unit of work inherits an enclosing one's credentials"
    (is (nil? (acting/run-as! "open_hax"
                             #(acting/run-as! nil (fn [] (acting/current-actor-id))))))
    (is (true? (acting/run-as! "open_hax"
                              #(acting/run-as! nil (fn [] (acting/in-scope?))))))
    (testing "and the outer actor is intact afterwards"
      (is (= "open_hax"
             (acting/run-as! "open_hax" (fn []
                                         (acting/run-as! nil (fn [] nil))
                                         (acting/current-actor-id))))))))

(deftest scope-nests-innermost-first
  (is (= "inner"
         (acting/run-as! "outer" #(acting/run-as! "inner" (fn [] (acting/current-actor-id))))))
  (testing "and the outer scope is intact afterwards"
    (is (= "outer"
           (acting/run-as! "outer" (fn []
                                    (acting/run-as! "inner" (fn [] nil))
                                    (acting/current-actor-id)))))))

;; ─────────────────────────────────────────────────────────
;; The reason this is AsyncLocalStorage and not an atom.
;;
;; agent-context — the existing actor carrier — is a process-global atom whose
;; own docstring says "best-effort, per-process". On a concurrent HTTP surface
;; two calls interleave at every await, so a global would hand one caller the
;; other's actor: a cross-tenant credential read that no single-request test can
;; find. These two tests fail against an atom and pass against a real scope.
;; ─────────────────────────────────────────────────────────

(defn- ^:async tick
  "Yield to the microtask queue, the way any awaited I/O would."
  []
  (await (js/Promise.resolve nil)))

(defn- ^:async actor-after-awaits
  "The actor still in scope after two yields — one await per hop, as a
   credential lookup would do."
  []
  (await (tick))
  (await (tick))
  (acting/current-actor-id))

(defn- observe-actor
  "Enter a scope and report what it observes once its awaits have resolved."
  [actor]
  (acting/run-as! actor actor-after-awaits))

(deftest scope-survives-an-await
  (async done
    ((^:async fn []
       (let [seen (await (observe-actor "open_hax"))]
         (is (= "open_hax" seen)
             "the actor must still be readable after the awaits a credential
              lookup performs")
         (done))))))

(deftest concurrent-scopes-do-not-observe-each-other
  (async done
    ((^:async fn []
       ;; All started before any resolves, so their awaits interleave.
       (let [pending #js [(observe-actor "open_hax")
                          (observe-actor "discord_automation")
                          (observe-actor "chat_primary")]
             seen    (await (js/Promise.all pending))]
         (is (= ["open_hax" "discord_automation" "chat_primary"] (vec seen))
             "each interleaved call must observe only its own actor")
         (done))))))

;; ─────────────────────────────────────────────────────────
;; A token gets an actor only if it carries one.
;;
;; token-actor-honourable? admits an actor-less token so that tokens minted
;; before this change keep working. That admission must not become a *grant*:
;; if an actor-less token were then scoped to whatever its membership resolves
;; to now, every already-issued token would silently gain the power to post as
;; that actor on Discord and Bluesky, without its consent screen ever having
;; named one. Honourable means "not refused", not "entitled".
;;
;; This is the rule call-actor-id implements; pinned here because the two
;; predicates read as if honourable implied granted.
;; ─────────────────────────────────────────────────────────

(defn- scoped-actor
  "What call-actor-id resolves for a token/membership pair, as law decides it.

   Mirrors the route: refuse a mismatch, then take the membership's actor only
   when the token itself carries one."
  [token-actor membership-actor]
  (when-not (law/token-actor-honourable? token-actor membership-actor)
    (throw (js/Error. "actor_reassigned")))
  (when (acting/normalize-actor-id token-actor)
    (acting/normalize-actor-id membership-actor)))

(deftest a-token-carrying-an-actor-is-scoped-to-it
  (is (= "open_hax" (scoped-actor "open_hax" "open_hax"))))

(deftest a-legacy-token-is-not-upgraded-to-the-memberships-actor
  (testing "no actorId means no actor scope, even though the membership has one"
    (is (nil? (scoped-actor nil "open_hax")))
    (is (nil? (scoped-actor "" "open_hax")))
    (is (nil? (scoped-actor "   " "open_hax")))))

(deftest a-reassigned-membership-refuses-rather-than-switching
  (is (thrown? js/Error (scoped-actor "open_hax" "discord_automation"))
      "the call must be refused, not quietly re-pointed at the new actor")
  (is (thrown? js/Error (scoped-actor "open_hax" nil))
      "clearing the membership's actor must refuse too"))

;; ─────────────────────────────────────────────────────────
;; law/consent-actor-unchanged? — the window between page and click
;;
;; The consent page shows which actor the token will act as. An admin can
;; reassign or clear that actor while the page sits open, and the confirmation
;; recomputes from a fresh context — so without this rule a token is minted for
;; an actor the page never showed, and it is then honoured on every call because
;; it matches the membership. The user consents to posting from one account and
;; gets another.
;;
;; The displayed value arrives from a form field, so it is client-controlled. It
;; is a witness of what was shown, never identity: the minted actor is always
;; the context's. A forged match asserts "nothing changed", which is either true
;; or is the very mismatch it was hiding — so a client can cause a refusal and
;; never an escalation.
;; ─────────────────────────────────────────────────────────

(deftest consent-actor-unchanged-when-it-matches
  (is (law/consent-actor-unchanged? "open_hax" "open_hax"))
  (testing "and tolerates whitespace from the form round-trip"
    (is (law/consent-actor-unchanged? "  open_hax " "open_hax"))))

(deftest consent-actor-changed-when-reassigned
  (is (not (law/consent-actor-unchanged? "open_hax" "discord_automation"))
      "the page showed one account; the membership now posts from another"))

(deftest consent-actor-changed-when-cleared
  (is (not (law/consent-actor-unchanged? "open_hax" nil)))
  (is (not (law/consent-actor-unchanged? "open_hax" ""))))

(deftest consent-actor-changed-when-one-appears
  (testing "an actor appearing is as much a change as one being replaced: the
            page warned that credential-backed tools would fail, and the user
            consented to that"
    (is (not (law/consent-actor-unchanged? "" "open_hax")))
    (is (not (law/consent-actor-unchanged? nil "open_hax")))))

(deftest no-actor-either-side-is-unchanged
  (testing "a membership with no actor renders the no-actor warning, and
            consenting to that is legitimate — it must not be refused"
    (is (law/consent-actor-unchanged? nil nil))
    (is (law/consent-actor-unchanged? "" ""))
    (is (law/consent-actor-unchanged? "   " nil))))

;; ─────────────────────────────────────────────────────────
;; The fallback must not be reachable from inside a scope.
;;
;; domain.actor.credentials/current-actor-id has two sources: the scope, and the
;; process-global agent-context that the agent-spawn path sets. An actor-less MCP
;; call is inside a scope that names no actor, and a concurrent agent turn may
;; have left an unrelated actor in agent-context. If absence in the scope fell
;; through to that global, the actor-less token would spend that turn's Discord
;; and Bluesky credentials — with every visible check passing.
;;
;; Exercised through the real credentials fn, with agent-context genuinely set,
;; because this is precisely the interaction between the two that no test of
;; either alone can catch.
;; ─────────────────────────────────────────────────────────

(defn- with-agent-context
  "Run f with agent-context holding an actor, then clear it.

   set-context! only stores a context when session-id and conversation-id are
   both present, so they are supplied."
  [actor f]
  (agent-context/set-context! {:session-id "s-1"
                               :conversation-id "c-1"
                               :agent-spec {:actor-id actor}})
  (try (f) (finally (agent-context/clear-context!))))

(deftest agent-context-supplies-the-actor-outside-any-scope
  (testing "the agent-spawn path must keep working unchanged"
    (with-agent-context "chat_primary"
      (fn [] (is (= "chat_primary" (credentials/current-actor-id)))))))

(deftest an-actor-less-scope-does-not-fall-back-to-agent-context
  (with-agent-context "chat_primary"
    (fn []
      (is (nil? (acting/run-as! nil #(credentials/current-actor-id)))
          "an actor-less MCP call must not borrow a concurrent agent turn's actor")
      (is (nil? (acting/run-as! "" #(credentials/current-actor-id)))))))

(deftest a-scoped-actor-outranks-agent-context
  (with-agent-context "chat_primary"
    (fn []
      (is (= "open_hax" (acting/run-as! "open_hax" #(credentials/current-actor-id)))
          "the narrower per-call claim must beat the process-global"))))

(deftest agent-context-is-restored-after-a-scope
  (with-agent-context "chat_primary"
    (fn []
      (acting/run-as! "open_hax" (fn [] nil))
      (is (= "chat_primary" (credentials/current-actor-id))
          "a scope must not disturb the agent turn it ran inside"))))
