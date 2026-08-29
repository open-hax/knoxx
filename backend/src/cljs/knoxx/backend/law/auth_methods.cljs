(ns knoxx.backend.law.auth-methods
  "Which authentication method, if any, admits a request to a surface.

   The contract under contracts/authentication/ says what is allowed; this
   namespace decides whether a particular request satisfies it. Both halves are
   data-in, data-out: no env, no socket, no clock. infra.auth.method-config
   reads those and hands the facts in.

   Fail closed everywhere. An absent contract, an unparseable one, a method
   with no grant, a method whose guards are not satisfied — every one of them
   is a refusal, because the failure mode on the other side is an open MCP
   surface that nobody reading the repository can see is open."
  (:require [clojure.string :as str]))

(def loopback-addresses
  "Addresses that mean the caller shares this machine.

   ::ffff:127.0.0.1 is the IPv4-mapped form Node reports on a dual-stack
   listener; omitting it would refuse the ordinary `curl localhost` case."
  #{"127.0.0.1" "::1" "::ffff:127.0.0.1" "localhost"})

(def default-min-token-length
  "Floor for a shared-secret token when the contract names none.

   A one-character token turns a trusted method into something a stray
   Authorization header reaches by accident."
  8)

(defn loopback-address?
  "True when `address` names this machine."
  [address]
  (contains? loopback-addresses (-> (str (or address "")) str/trim str/lower-case)))

(defn- enabled?
  [method]
  (true? (:auth-method/enabled method)))

(defn methods-for-surface
  "Every method the contract declares for `surface`, enabled or not."
  [contract surface]
  (if (= surface (:auth/surface contract))
    (vec (:auth/methods contract))
    []))

(defn enabled-methods
  "The methods `surface` currently accepts."
  [contract surface]
  (filterv enabled? (methods-for-surface contract surface)))

(defn method-enabled?
  "True when `surface` accepts `method-id`."
  [contract surface method-id]
  (boolean (some #(= method-id (:auth-method/id %)) (enabled-methods contract surface))))

(defn- normalize-grant-tools
  [tools]
  (cond
    (= :all tools) :all
    (sequential? tools) (into [] (comp (map str) (map str/trim) (remove str/blank?)) tools)
    :else nil))

(defn grant-of
  "The principal a method hands to a request it accepts, or nil.

   nil for a method that grants nothing. A method that authenticates callers
   and names no identity would resolve a blank membership — a valid credential
   carrying no authorization — so it is treated as misconfigured rather than
   permissive."
  [method]
  (let [grants (:auth-method/grants method)
        email  (some-> (:grant/user-email grants) str str/trim not-empty)
        tools  (normalize-grant-tools (:grant/tools grants))]
    (when (and email tools)
      (cond-> {:user-email email :tools tools}
        (some-> (:grant/org-slug grants) str str/trim not-empty)
        (assoc :org-slug (str/trim (:grant/org-slug grants)))
        (some-> (:grant/actor-id grants) str str/trim not-empty)
        (assoc :actor-id (str/trim (:grant/actor-id grants)))))))

(defn- token-satisfied?
  [method {:keys [configured-token presented-token]}]
  (let [minimum   (or (:auth-method/min-token-length method) default-min-token-length)
        configured (str/trim (str (or configured-token "")))
        presented  (str/trim (str (or presented-token "")))]
    (and (>= (count configured) minimum)
         (= configured presented))))

(defn- guards-satisfied?
  [method {:keys [remote-address production?] :as request}]
  (and (or (not (:auth-method/require-non-production method))
           (not production?))
       (or (not (:auth-method/require-loopback method))
           (loopback-address? remote-address))
       (token-satisfied? method request)))

(defn trusted-loopback-grant
  "The grant admitting this request under :trusted-loopback, or nil.

   `request` carries {:configured-token :presented-token :remote-address
   :production?}. Every guard the method declares must hold, and the method
   must be enabled on this surface at all.

   Compared with `=` rather than a constant-time equality: a method that
   requires loopback is already refused for anyone not on this machine, and a
   party who can time responses on our own loopback interface can read the
   token's source directly."
  [contract surface request]
  (when-let [method (->> (enabled-methods contract surface)
                         (some #(when (= :trusted-loopback (:auth-method/id %)) %)))]
    (when (guards-satisfied? method request)
      (grant-of method))))
