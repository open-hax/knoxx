(ns knoxx.backend.domain.social.api-receipts
  "Interpret platform acknowledgments without inventing object identifiers."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.social.action-admission :as admission]
            [knoxx.backend.law.platform-action :as law]))

(defn- present?
  [value]
  (and (string? value) (not (str/blank? value))))

(defn- record-confirmed?
  [action response body]
  (let [batch? (seq (:response-path action))
        change (get-in action [:effect :mutation])
        expected (case change :create "createResult" :replace "updateResult" :delete "deleteResult")
        tag (str "com.atproto.repo.applyWrites#" expected)
        uri (get body "uri")
        parts (when (string? uri) (re-matches #"at://(did:[^/]+)/([^/]+)/([^/?#]+)" uri))]
    (and (or (not batch?) (= tag (get body "$type")))
         (if (= :delete change)
           true
           (and (present? (get body "cid")) parts
                (= (:authenticated-did response) (second parts))
                (= (get-in action [:effect :collection]) (nth parts 2)))))))

(defn- confirmed?
  [action response body]
  (and (:ok response)
       (case (:kind action)
         :discord-message (and (present? (get body "id"))
                               (= (if (= ["message"] (:response-path action)) (get-in response [:body "id"])
                                      (get-in action [:intent :target]))
                                  (get body "channel_id")))
         :discord-thread (present? (get body "id"))
         :discord-reaction true
         :bluesky-chat (present? (get body "id"))
         :bluesky-record (record-confirmed? action response body)
         :operation true)))

(defn outcomes
  "One actual outcome per effect, including missing batch results. Success HTTP
   alone cannot confirm a created post/message. The shared port settles these."
  [actions response]
  (mapv (fn [action]
          (let [body (get-in (:body response) (:response-path action))
                status (if (confirmed? action response body) :confirmed
                           (if (:ok response) :unknown
                               (:status (admission/outcome (:intent action) {:failed? true :details {:status (:status response)}}))))
                code (or (get-in response [:body "code"]) (get-in response [:body "error"]))
                outcome (cond-> {:status status :http-status (:status response)}
                          (or (string? code) (integer? code)) (assoc :platform-code code)
                          (and (= ["message"] (:response-path action)) (present? (get-in response [:body "id"])))
                          (assoc :resolved-target (get-in response [:body "id"])))]
            (law/assert-valid! law/Outcome outcome))) actions))

(defn failure-outcomes
  "Transport failures retain typed evidence; no request-body text can prove a refusal."
  [actions error-data]
  (mapv #(admission/outcome (:intent %) {:failed? true :error-data error-data}) actions))

(defn publications
  "Canonical post receipts for native self-thread/source checks. Every item is
   tied to its actual authenticated repository response and submitted text."
  [actions response actual-outcomes]
  (into []
        (keep (fn [[action outcome]]
                (when (and (= :confirmed (:status outcome)) (= :bluesky-record (:kind action))
                           (= "app.bsky.feed.post" (get-in action [:effect :collection]))
                           (= "app.bsky.feed.post" (get-in action [:record "$type"]))
                           (not= :delete (get-in action [:effect :mutation])))
                  (let [body (get-in (:body response) (:response-path action)) record (:record action)]
                    {:uri (get body "uri") :cid (get body "cid") :text (get record "text")
                     :reply_to (get-in record ["reply" "parent" "uri"] "")
                     :replyRoot (or (get-in record ["reply" "root"])
                                    {:uri (get body "uri") :cid (get body "cid")})
                     :replyParent (get-in record ["reply" "parent"])
                     :actorDid (:authenticated-did response)}))))
        (map vector actions actual-outcomes)))
