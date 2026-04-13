(ns knoxx.backend.app-shapes
  (:require [clojure.string :as str]))

(defn- normalize-tool-policy
  [policy]
  (let [tool-id (some-> (or (:toolId policy)
                            (:tool-id policy)
                            (:tool_id policy))
                        str
                        str/trim
                        not-empty)
        effect (some-> (or (:effect policy) "allow")
                       str
                       str/trim
                       str/lower-case
                       not-empty)]
    (when tool-id
      {:toolId tool-id
       :effect (if (#{"allow" "deny"} effect)
                 effect
                 "allow")})))

(defn- normalize-tool-policies
  [policies]
  (vec (keep normalize-tool-policy (or policies []))))

(defn- normalize-agent-spec
  [value]
  (let [spec (some-> value (js->clj :keywordize-keys true))
        role (some-> (or (:role spec) (:role_slug spec) (:role-slug spec)) str str/trim not-empty)
        system-prompt (some-> (or (:system_prompt spec)
                                  (:system-prompt spec)
                                  (:systemPrompt spec))
                              str
                              not-empty)
        model (some-> (:model spec) str str/trim not-empty)
        thinking-level (some-> (or (:thinking_level spec)
                                   (:thinking-level spec)
                                   (:thinkingLevel spec)
                                   (:reasoning_effort spec)
                                   (:reasoning-effort spec)
                                   (:reasoningEffort spec))
                               str
                               str/trim
                               not-empty)
        tool-policies (normalize-tool-policies (or (:tool_policies spec)
                                                   (:tool-policies spec)
                                                   (:toolPolicies spec)))
        resource-policies (or (:resource_policies spec)
                              (:resource-policies spec)
                              (:resourcePolicies spec))]
    (when (or role system-prompt model thinking-level (seq tool-policies) resource-policies)
      {:role role
       :system-prompt system-prompt
       :model model
       :thinking-level thinking-level
       :tool-policies tool-policies
       :resource-policies resource-policies})))

(defn normalize-chat-body
  [body]
  {:message (or (aget body "message") "")
   :conversation-id (or (aget body "conversationId")
                        (aget body "conversation_id"))
   :session-id (or (aget body "sessionId")
                   (aget body "session_id"))
   :run-id (or (aget body "runId")
               (aget body "run_id"))
   :model (or (aget body "model") nil)
   :thinking-level (or (aget body "thinkingLevel")
                       (aget body "thinking_level")
                       (aget body "reasoningEffort")
                       (aget body "reasoning_effort"))
   :mode (or (aget body "mode") "direct")
   :agent-spec (normalize-agent-spec (or (aget body "agentSpec")
                                         (aget body "agent_spec")))
   :auth-context (some-> (or (aget body "authContext")
                             (aget body "auth_context"))
                         (js->clj :keywordize-keys true))})

(defn normalize-control-body
  [body]
  {:message (or (aget body "message") "")
   :conversation-id (or (aget body "conversationId")
                        (aget body "conversation_id"))
   :session-id (or (aget body "sessionId")
                   (aget body "session_id"))
   :run-id (or (aget body "runId")
               (aget body "run_id"))})

(defn route!
  [app method url handler]
  (.route app #js {:method method
                   :url url
                   :handler handler}))
