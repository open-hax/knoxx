(ns knoxx.backend.law.oidc
  "Admission laws for shared identity; product authority remains in Knoxx."
  (:require [clojure.string :as str]))

(defn nonblank?
  "Accept a nonblank string, without coercing identities."
  [value]
  (and (string? value) (not (str/blank? value))))

(defn require-config!
  "Reject incomplete or ambiguous operator-owned identity links. URI checks live at the extern."
  [{:keys [issuer public-origin client-id client-secret links] :as config}]
  (when-not (and (every? nonblank? [issuer public-origin client-id client-secret])
                 (vector? links)
                 (every? #(and (= issuer (:issuer %))
                                (every? nonblank? ((juxt :subject :user-id :membership-id :org-id) %)))
                         links)
                 (= (count links) (count (distinct (map (juxt :issuer :subject) links)))))
    (throw (ex-info "Invalid Axxium OIDC configuration" {:status 503 :code :oidc-config})))
  config)

(defn approved-link!
  "Select only a qualified, explicit link. Email, names and remote roles are never evidence."
  [links issuer subject]
  (or (some #(when (and (= issuer (:issuer %)) (= subject (:subject %))) %) links)
      (throw (ex-info "This Axxium identity needs an approved Knoxx account link. Contact your operator; email matching is not permitted."
                      {:status 403 :code :oidc-link-required}))))

(defn require-context!
  "Bind the approved local IDs to a fresh Knoxx policy context without creating an actor."
  [link ctx]
  (when-not (and (= (:user-id link) (get-in ctx [:user :id]))
                 (= (:membership-id link) (get-in ctx [:membership :id]))
                 (= (:org-id link) (get-in ctx [:org :id]))
                 (nonblank? (get-in ctx [:actor :binding]))
                 (= (get-in ctx [:actor :binding]) (get-in ctx [:actor :id]))
                 (= (get-in ctx [:actor :binding]) (get-in ctx [:membership :actor-id])))
    (throw (ex-info "Approved Axxium link does not match the current Knoxx membership"
                    {:status 403 :code :oidc-link-conflict})))
  ctx)

(defn require-grant!
  "Require current client-scoped introspection and a still-live validated ID token.
   RFC7662 aud is optional for this identity-only grant; if present it must match.
   client_id is mandatory because the authority scopes introspection to that client."
  [{:keys [issuer client-id]} {:keys [subject expires-at]} evidence now-ms]
  (let [aud (:aud evidence)
        audiences (if (sequential? aud) aud [aud])]
    (when-not (and (true? (:active evidence))
                   (= client-id (:client_id evidence))
                   (= subject (:sub evidence))
                   (or (nil? (:iss evidence)) (= issuer (:iss evidence)))
                   (or (nil? aud) (some #{client-id} audiences))
                   (string? (:scope evidence))
                   (contains? (set (re-seq #"\S+" (:scope evidence))) "openid")
                   (number? (:exp evidence)) (< now-ms (* 1000 (:exp evidence)))
                   (number? expires-at) (< now-ms expires-at))
      (throw (ex-info "Axxium login expired or was revoked. Sign in again."
                      {:status 401 :code :oidc-grant-invalid}))))
  evidence)

(defn require-fresh-evidence!
  "Reserve at most five seconds for introspection plus local policy resolution.
   Together with the 25-second upgraded transport lease this caps admission at 30 seconds."
  [started now]
  (when-not (<= 0 (- now started) 4999)
    (throw (ex-info "Axxium admission evidence took too long. Sign in again."
                    {:status 503 :code :oidc-evidence-stale}))))

(def session-limit-ms "Finite browser admission bound, independent of token renewal." 86400000)
(def refresh-window-ms "Refresh before normal token expiry." 30000)

(defn session-bound
  "A renewable browser session requires the authority's current stored parent bound."
  [grant evidence now]
  (if (:refresh-token grant)
    (let [bound (:root_session_expires_at evidence)]
      (when-not (and (number? bound) (< now (* bound 1000)))
        (throw (ex-info "Axxium root session bound is missing" {:status 401 :code :oidc-grant-invalid})))
      (min (+ now session-limit-ms) (* bound 1000)))
    (:expires-at grant)))

(defn require-renewal!
  "Renewal preserves subject and original nonce; nonce may be omitted by the OP."
  [previous renewed]
  (when-not (and (= (:subject previous) (:subject renewed))
                 (nonblank? (:refresh-token renewed))
                 (not= (:refresh-token previous) (:refresh-token renewed))
                 (or (nil? (:nonce renewed)) (= (:nonce previous) (:nonce renewed))))
    (throw (ex-info "Shared login renewal refused" {:status 401 :code :oidc-grant-invalid})))
  renewed)

(defn device-poll-admission
  "Pure browser transaction admission, separate from the engine's OAuth polling policy."
  [tx now]
  (cond
    (nil? tx) :refuse
    (<= (:expires-at tx) now) :expired
    (= "completing" (:status tx)) :refuse
    (< now (:next-poll tx)) :slow
    :else :continue))

(defn require-browser-origin!
  "Require exact product origin for cookie-authenticated state changes."
  [{:keys [public-origin]} {:keys [method origin]}]
  (when-not (or (contains? #{"GET" "HEAD" "OPTIONS"} method)
                (= public-origin origin))
    (throw (ex-info "Shared login requires the configured Knoxx origin"
                    {:status 403 :code :oidc-origin-invalid}))))

(defn require-session-continuity!
  "Renewal may replace tokens; logout, rebinding and the current deadline still fence admission."
  [attempted current now]
  (when-not (and current
                 (= (select-keys attempted [:subject :link :nonce])
                    (select-keys current [:subject :link :nonce]))
                 (< now (or (:session-expires-at current) (:expires-at current))))
    (throw (ex-info "Axxium session was signed out" {:status 401 :code :oidc-session-expired})))
  current)

(defn pending-evictions
  "Choose oldest unadmitted entries to make one bounded slot, including equal-deadline ties."
  [entries limit]
  (->> entries (sort-by (comp :expires-at val))
       (take (max 0 (inc (- (count entries) limit)))) (mapv key)))
