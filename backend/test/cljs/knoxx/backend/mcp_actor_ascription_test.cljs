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
            [knoxx.backend.infra.actor.scope :as scope]
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
  (is (nil? (scope/current-actor-id))))

(deftest scope-reads-inside-a-run
  (is (= "open_hax" (scope/run-as! "open_hax" #(scope/current-actor-id)))))

(deftest scope-does-not-outlive-a-run
  (scope/run-as! "open_hax" (fn [] nil))
  (is (nil? (scope/current-actor-id))
      "a scope must not leak into the next unit of work"))

(deftest scope-normalizes-and-refuses-blanks
  (testing "an id arrives trimmed"
    (is (= "open_hax" (scope/run-as! "  open_hax\n" #(scope/current-actor-id)))))
  (testing "a blank enters no scope at all, so the failure surfaces at the
            credential read rather than as a lookup for actor \"\""
    (is (nil? (scope/run-as! "" #(scope/current-actor-id))))
    (is (nil? (scope/run-as! "   " #(scope/current-actor-id))))
    (is (nil? (scope/run-as! nil #(scope/current-actor-id))))))

(deftest scope-nests-innermost-first
  (is (= "inner"
         (scope/run-as! "outer" #(scope/run-as! "inner" (fn [] (scope/current-actor-id))))))
  (testing "and the outer scope is intact afterwards"
    (is (= "outer"
           (scope/run-as! "outer" (fn []
                                    (scope/run-as! "inner" (fn [] nil))
                                    (scope/current-actor-id)))))))

;; ─────────────────────────────────────────────────────────
;; The reason this is AsyncLocalStorage and not an atom.
;;
;; agent-context — the existing actor carrier — is a process-global atom whose
;; own docstring says "best-effort, per-process". On a concurrent HTTP surface
;; two calls interleave at every await, so a global would hand one caller the
;; other's actor: a cross-tenant credential read that no single-request test can
;; find. These two tests fail against an atom and pass against a real scope.
;; ─────────────────────────────────────────────────────────

(defn- tick
  "Yield to the microtask queue, the way any awaited I/O would."
  []
  (js/Promise.resolve nil))

(deftest scope-survives-an-await
  (async done
    (-> (scope/run-as! "open_hax"
                       (fn []
                         (-> (tick)
                             (.then (fn [_] (tick)))
                             (.then (fn [_] (scope/current-actor-id))))))
        (.then (fn [seen]
                 (is (= "open_hax" seen)
                     "the actor must still be readable after the awaits a
                      credential lookup performs")
                 (done))))))

(deftest concurrent-scopes-do-not-observe-each-other
  (async done
    (let [observe (fn [actor]
                    (scope/run-as! actor
                                   (fn []
                                     (-> (tick)
                                         (.then (fn [_] (tick)))
                                         (.then (fn [_] (scope/current-actor-id)))))))]
      ;; Started before either resolves, so their awaits interleave.
      (-> (js/Promise.all #js [(observe "open_hax")
                               (observe "discord_automation")
                               (observe "chat_primary")])
          (.then (fn [seen]
                   (is (= ["open_hax" "discord_automation" "chat_primary"]
                          (vec seen))
                       "each interleaved call must observe only its own actor")
                   (done)))))))

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
  (when (scope/normalize-actor-id token-actor)
    (scope/normalize-actor-id membership-actor)))

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
