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
