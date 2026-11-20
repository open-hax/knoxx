(ns knoxx.backend.domain.social.api-json-request
  "Plan pinned JSON operations. Planning does not grant or execute a write."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.social.api-effects :as effects]
            [knoxx.backend.domain.social.api-request :as bindings]
            [knoxx.backend.law.platform-api-effects :as effect-law]
            [knoxx.backend.law.platform-api-request :as law]
            [knoxx.backend.law.platform-payload :as payload]))

(defn- refuse!
  [code message]
  (throw (ex-info message {:code code :phase :preflight})))

(defn- credential-field?
  [field]
  (boolean (re-find #"(?:^|_)(?:token|jwt|password|secret|authorization)(?:$|_)"
                    (-> field (str/replace #"([a-z])([A-Z])" "$1_$2") str/lower-case))))

(defn- assert-no-credentials!
  [value]
  (cond
    (map? value) (doseq [[field item] value]
                   (when (credential-field? field)
                     (refuse! "platform_body_credential" "Credentials must come from the acting identity's scoped vault"))
                   (assert-no-credentials! item))
    (vector? value) (doseq [item value] (assert-no-credentials! item))))

(defn- empty-statuses!
  [platform schema]
  (if (= platform "discord")
    (let [responses (filter #(re-matches #"2[0-9][0-9]" (key %)) (get schema "responses"))]
      (when (empty? responses) (refuse! "platform_success_schema_missing" "No pinned success response is available"))
      (doseq [[_ response] responses]
        (when (and (seq (get response "content"))
                   (not (contains? (get response "content") "application/json")))
          (refuse! "platform_response_encoding_unsupported" "Use the media/export path for non-JSON responses")))
      (mapv #(parse-long (key %)) (filter #(empty? (get (val %) "content")) responses)))
    (let [output (get schema "output")]
      (when (and output (not= "application/json" (get output "encoding")))
        (refuse! "platform_response_encoding_unsupported" "Use the media/export path for non-JSON responses"))
      (if output [] [200]))))

(defn- assert-body!
  [asset selected schema input]
  (let [discord? (= "discord" (:platform input))
        input-schema (if discord?
                       (get-in schema ["requestBody" "content" "application/json" "schema"])
                       (when (= "application/json" (get-in schema ["input" "encoding"]))
                         (get-in schema ["input" "schema"])))
        required? (if discord? (true? (get-in schema ["requestBody" "required"]))
                      (contains? schema "input"))]
    (when (and (or (contains? input :body) required? (contains? schema "requestBody")) (nil? input-schema))
      (refuse! "platform_request_encoding_unsupported" "This operation has no JSON request schema; use its binary, form or media interface"))
    (when (and required? (not (contains? input :body)))
      (refuse! "platform_body_required" "The operation requires its actual JSON body"))
    (when (contains? input :body)
      (payload/assert-body! asset (get selected "document") input-schema (:body input)))))

(defn json-plan
  "Validate operation/bindings/basic body shape and resolve actual effects before
   credential access. Returns separate transport plan and derived effect data;
   the orchestration layer must admit every effect before invoking transport."
  [asset input]
  (law/assert-valid! law/Input input)
  (let [{:keys [selected schema plan]} (bindings/operation-bindings asset input)
        platform (:platform input) body (:body input)
        _ (assert-body! asset selected schema input)
        analysis (effects/request-effects selected body)
        _ (effect-law/require-resolved! analysis)
        _ (when (some #(or (:credential? %) (:separate-auth? %)) (:actions analysis))
            (refuse! "platform_separate_auth_required" "This operation needs a separate credential workflow, not an ordinary actor JSON request"))
        _ (assert-no-credentials! body)
        empty-statuses (empty-statuses! platform schema)
        plan (cond-> (assoc plan :empty-success-statuses empty-statuses)
               (contains? input :body) (assoc :body body)
               (and (= "bluesky" platform) (contains? body "repo"))
               (assoc :account-repository (get body "repo")))]
    {:plan (law/assert-valid! law/Plan plan) :effects analysis}))
