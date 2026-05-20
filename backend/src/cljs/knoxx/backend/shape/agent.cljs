(ns knoxx.backend.shape.agent
  "Protocol for a running agent session.
   Implementations live under extern/ — no JS interop here.")

(defprotocol IAgentSession
  (streaming? [s]
    "True when the session has an active streaming turn.")
  (current-turn [s]
    "The current running turn object (opaque), or nil if idle.")
  (messages [s]
    "A seq of the session's message history as JS objects.
     Individual message structure is eta-mu's internal format;
     use infra.agent.transcript functions to convert.")
  (follow-up! [s message]
    "Queue a follow-up message into the running turn. Returns a Promise.")
  (steer! [s message]
    "Steer the current generation. Returns a Promise.")
  (set-thinking-level! [s level]
    "Set the thinking/reasoning level on the session."))
